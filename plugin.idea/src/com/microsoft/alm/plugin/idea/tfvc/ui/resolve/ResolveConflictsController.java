// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.common.ui.common.PageModel;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller for resolving conflicts interactively with users
 */
public class ResolveConflictsController implements Observer, ActionListener {
    private final ResolveConflictsDialog dialog;
    private final ResolveConflictsModel model;

    public ResolveConflictsController(final Project project, final ResolveConflictHelper conflictHelper) {
        this(new ResolveConflictsDialog(project), new ResolveConflictsModel(project, conflictHelper));
    }

    public ResolveConflictsController(final ResolveConflictsDialog dialog, final ResolveConflictsModel model) {
        this.dialog = dialog;
        this.model = model;

        this.dialog.addActionListener(this);
        this.model.addObserver(this);

        this.model.loadConflicts();
        update(null, null);
    }

    public boolean showModalDialog() {
        return dialog.showModalDialog();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateModel();

        if (ResolveConflictsForm.CMD_ACCEPT_THEIRS.equals(e.getActionCommand())) {
            model.acceptTheirs(dialog.getSelectedRows());
        } else if (ResolveConflictsForm.CMD_ACCEPT_YOURS.equals(e.getActionCommand())) {
            model.acceptYours(dialog.getSelectedRows());
        } else if (ResolveConflictsForm.CMD_MERGE.equals(e.getActionCommand())) {
            model.merge(dialog.getSelectedRows());
        } else if (BaseDialog.CMD_OK.equals(e.getActionCommand())) {
            model.processSkippedConflicts();
        }
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (arg == null) {
            dialog.setConflictsTableModel(model.getConflictsTableModel());
        }

        if (PageModel.PROP_ERRORS.equals(arg)) {
            if (model.hasErrors()) {
                dialog.displayError(model.getErrors().get(0).getValidationMessage());
            } else {
                dialog.displayError(null);
            }
        }
    }

    protected void updateModel() {
        // clear current errors, if still existing then they will reappear
        model.clearErrors();
    }
}