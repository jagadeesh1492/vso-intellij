/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.LabelItemSpec;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.VersionControlLabel;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.MessageFormat;
import java.util.List;

public class ApplyLabelDialog extends DialogWrapper {

  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private final String mySourcePath;

  private ApplyLabelForm myApplyLabelForm;

  public ApplyLabelDialog(final Project project, final WorkspaceInfo workspace, final String sourcePath) {
    super(project, true);
    myProject = project;
    myWorkspace = workspace;
    mySourcePath = sourcePath;

    setTitle("Apply Label");

    init();
    setOKActionEnabled(false);
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myApplyLabelForm = new ApplyLabelForm(myProject, myWorkspace, mySourcePath);

    getWindow().addComponentListener(new ComponentAdapter() {
      public void componentShown(final ComponentEvent e) {
        myApplyLabelForm.addItems();
      }
    });

    myApplyLabelForm.addListener(new ApplyLabelForm.Listener() {
      public void dataChanged(final String labelName, final int visibleItemsCount) {
        setOKActionEnabled(visibleItemsCount > 0 && labelName.length() > 0);
      }
    });

    return myApplyLabelForm.getContentPane();
  }

  protected void doOKAction() {
    try {
      List<VersionControlLabel> labels = myWorkspace.getServer().getVCS()
        .queryLabels(getLabelName(), null, null, false, null, null, false, myApplyLabelForm.getContentPane(),
                     TFSBundle.message("checking.existing.labels"));
      if (!labels.isEmpty()) {
        String message = MessageFormat.format("Label ''{0}'' already exists.\nDo you want to update it?", getLabelName());
        if (Messages.showOkCancelDialog(myProject, message, getTitle(), "Update Label", "Cancel", Messages.getQuestionIcon()) !=
            Messages.OK) {
          return;
        }
      }
    }
    catch (TfsException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), getTitle());
      return;
    }
    super.doOKAction();
  }

  public String getLabelName() {
    return myApplyLabelForm.getLabelName();
  }

  public String getLabelComment() {
    return myApplyLabelForm.getLabelComment();
  }

  public List<LabelItemSpec> getLabelItemSpecs() {
    return myApplyLabelForm.getLabelItemSpecs();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.ApplyLabel";
  }

}
