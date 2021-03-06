// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts;

import com.intellij.openapi.project.Project;
import com.intellij.util.WaitForProgressToShow;
import com.microsoft.alm.plugin.idea.tfvc.ui.resolve.ResolveConflictsController;

/**
 * Handles conflicts interactively with the user to help resolve them
 */
public class DialogConflictsHandler implements ConflictsHandler {
    public void resolveConflicts(final Project project, final ResolveConflictHelper conflictHelper) {
        WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(new Runnable() {
            public void run() {
                final ResolveConflictsController controller = new ResolveConflictsController(project, conflictHelper);
                controller.showModalDialog();
            }
        });
    }
}
