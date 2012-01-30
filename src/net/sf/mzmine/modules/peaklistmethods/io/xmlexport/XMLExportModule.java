/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.io.xmlexport;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class XMLExportModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Export to XML file";
    private static final String MODULE_DESCRIPTION = "This method exports the peak list contents into an XML file.";

    @Override
    public String getName() {
	return MODULE_NAME;
    }

    @Override
    public String getDescription() {
	return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull ParameterSet parameters,
	    @Nonnull Collection<Task> tasks) {
	XMLExportTask newTask = new XMLExportTask(parameters);
	tasks.add(newTask);
	return ExitCode.OK;

    }

    @Override
    public MZmineModuleCategory getModuleCategory() {
	return MZmineModuleCategory.PEAKLISTEXPORT;
    }

    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
	return XMLExportParameters.class;
    }

}
