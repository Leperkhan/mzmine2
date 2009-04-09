/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.batchmode;

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Batch mode task
 */
public class BatchTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus taskStatus = TaskStatus.WAITING;

	private String errorMessage;
	private int totalSteps, processedSteps;

	private BatchQueue queue;

	private RawDataFile dataFiles[];
	private PeakList peakLists[];

	BatchTask(BatchQueue queue, RawDataFile dataFiles[], PeakList peakLists[]) {
		this.queue = queue;
		this.dataFiles = dataFiles;
		this.peakLists = peakLists;
		totalSteps = queue.size();
	}

	public void run() {

		taskStatus = TaskStatus.PROCESSING;
		logger.info("Starting a batch of " + totalSteps + " steps");

		for (int i = 0; i < totalSteps; i++) {

			processQueueStep(i);
			processedSteps++;

			// If we are canceled or ran into error, stop here
			if ((taskStatus == TaskStatus.CANCELED)
					|| (taskStatus == TaskStatus.ERROR)) {
				return;
			}

		}

		logger.info("Finished a batch of " + totalSteps + " steps");
		taskStatus = TaskStatus.FINISHED;

	}

	private void processQueueStep(int stepNumber) {

		// Run next step of the batch
		BatchStepWrapper currentStep = queue.get(stepNumber);
		BatchStep method = currentStep.getMethod();
		ParameterSet batchStepParameters = currentStep.getParameters();

		Task[] currentStepTasks = method.runModule(dataFiles, peakLists,
				batchStepParameters);

		// If current step didn't produce any tasks, continue with next step
		if ((currentStepTasks == null) || (currentStepTasks.length == 0))
			return;

		boolean allTasksFinished = false;

		while (!allTasksFinished) {

			// If we canceled the batch, cancel all running tasks
			if (taskStatus == TaskStatus.CANCELED) {
				for (Task stepTask : currentStepTasks)
					stepTask.cancel();
				return;
			}

			// First set to true, then check all tasks
			allTasksFinished = true;

			for (Task stepTask : currentStepTasks) {

				TaskStatus stepStatus = stepTask.getStatus();

				// If any of them is not finished, keep checking
				if (stepStatus != TaskStatus.FINISHED)
					allTasksFinished = false;

				// If there was an error, we have to stop the whole batch
				if (stepStatus == TaskStatus.ERROR) {
					taskStatus = TaskStatus.ERROR;
					errorMessage = stepTask.getErrorMessage();
					return;
				}

				// If user canceled any of the tasks, we have to cancel the
				// whole batch
				if (stepStatus == TaskStatus.CANCELED) {
					taskStatus = TaskStatus.CANCELED;
					return;
				}

			}

			// Wait 1s before checking the tasks again
			if (!allTasksFinished) {
				synchronized (this) {
					try {
						this.wait(1000);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}

		}

		// Now all tasks are finished. We have to check if project was modified.
		// If any raw data files or peak lists were added, we continue batch
		// processing on those.

		Vector<RawDataFile> newDataFiles = new Vector<RawDataFile>();
		Vector<PeakList> newPeakLists = new Vector<PeakList>();

		for (Task stepTask : currentStepTasks) {
			Object createdObjects[] = stepTask.getCreatedObjects();
			if (createdObjects == null)
				continue;
			for (Object createdObject : createdObjects) {
				if (createdObject instanceof RawDataFile)
					newDataFiles.add((RawDataFile) createdObject);
				if (createdObject instanceof PeakList)
					newPeakLists.add((PeakList) createdObject);
			}
		}

		if (newDataFiles.size() > 0)
			dataFiles = newDataFiles.toArray(new RawDataFile[0]);
		if (newPeakLists.size() > 0)
			peakLists = newPeakLists.toArray(new PeakList[0]);

	}

	public void cancel() {
		taskStatus = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		if (totalSteps == 0)
			return 0;
		return (double) processedSteps / totalSteps;
	}

	public TaskStatus getStatus() {
		return taskStatus;
	}

	public String getTaskDescription() {
		return "Batch of " + totalSteps + " steps";
	}

	public Object[] getCreatedObjects() {
		return null;
	}

}