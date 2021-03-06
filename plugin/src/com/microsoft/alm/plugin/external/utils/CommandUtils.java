// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.utils;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.FindConflictsCommand;
import com.microsoft.alm.plugin.external.commands.FindWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.GetLocalPathCommand;
import com.microsoft.alm.plugin.external.commands.GetWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.HistoryCommand;
import com.microsoft.alm.plugin.external.commands.RenameCommand;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.external.commands.StatusCommand;
import com.microsoft.alm.plugin.external.commands.SyncCommand;
import com.microsoft.alm.plugin.external.commands.UndoCommand;
import com.microsoft.alm.plugin.external.commands.UpdateWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.UpdateWorkspaceMappingCommand;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.external.models.ConflictResults;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.RenameConflict;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper for running commands
 */
public class CommandUtils {

    /**
     * This method will return just the workspace name or empty string (never null)
     *
     * @param context
     * @param project
     * @return
     */
    public static String getWorkspaceName(final ServerContext context, final Project project) {
        ArgumentHelper.checkNotNull(project, "project");
        final FindWorkspaceCommand command = new FindWorkspaceCommand(project.getBasePath());
        final Workspace workspace = command.runSynchronously();
        if (workspace != null) {
            return workspace.getName();
        }
        return StringUtils.EMPTY;
    }

    /**
     * This method determines the workspace name from the project and then calls getWorkspace with the name.
     *
     * @param context
     * @param project
     * @return
     */
    public static Workspace getWorkspace(final ServerContext context, final Project project) {
        final String workspaceName = getWorkspaceName(context, project);
        return getWorkspace(context, workspaceName);
    }

    /**
     * This method returns the fully filled out Workspace object.
     *
     * @param context
     * @param workspaceName
     * @return
     */
    public static Workspace getWorkspace(final ServerContext context, final String workspaceName) {
        final GetWorkspaceCommand command = new GetWorkspaceCommand(context, workspaceName);
        return command.runSynchronously();
    }

    public static String getLocalPathSynchronously(final ServerContext context, final String serverPath, final String workspace) {
        final Command<String> getLocalPathCommand = new GetLocalPathCommand(context, serverPath, workspace);
        return getLocalPathCommand.runSynchronously();
    }

    public static List<ChangeSet> getHistoryCommand(final ServerContext context, final String localPath, final String version,
                                                    final int stopAfter, final boolean recursive, final String user) {
        return getHistoryCommand(context, localPath, version, stopAfter, recursive, user, false);
    }

    public static List<ChangeSet> getHistoryCommand(final ServerContext context, final String localPath, final String version,
                                                    final int stopAfter, final boolean recursive, final String user, final boolean itemMode) {
        final Command<List<ChangeSet>> historyCommand = new HistoryCommand(context, localPath, version, stopAfter, recursive, user, itemMode);
        return historyCommand.runSynchronously();
    }

    public static ChangeSet getLastHistoryEntryForAnyUser(final ServerContext context, final String localPath) {
        final List<ChangeSet> results = getHistoryCommand(context, localPath, null, 1, false, StringUtils.EMPTY);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * This command updates the properies of the workspace as well as the mappings.
     * There are many commands that go into the update, not just a single call.
     * If anything goes wrong, an exception will be thrown.
     * Note: this method does NOT sync the workspace.
     *
     * @param context
     * @param oldWorkspace
     * @param newWorkspace
     */
    public static void updateWorkspace(final ServerContext context, final Workspace oldWorkspace, final Workspace newWorkspace) {
        // No need to update the mappings if they are the same
        if (WorkspaceHelper.areMappingsDifferent(oldWorkspace, newWorkspace)) {
            // First remove the mappings that are no longer needed
            for (final Workspace.Mapping m : WorkspaceHelper.getMappingsToRemove(oldWorkspace, newWorkspace)) {
                final UpdateWorkspaceMappingCommand command = new UpdateWorkspaceMappingCommand(context, oldWorkspace.getName(), m, true);
                command.runSynchronously();
            }

            // Now update the mappings to match the new workspace
            for (final Workspace.Mapping m : WorkspaceHelper.getMappingsToChange(oldWorkspace, newWorkspace)) {
                final UpdateWorkspaceMappingCommand command = new UpdateWorkspaceMappingCommand(context, oldWorkspace.getName(), m, false);
                command.runSynchronously();
            }
        }

        // Finally update the properties of the workspace
        final UpdateWorkspaceCommand updateWorkspaceCommand = new UpdateWorkspaceCommand(context, oldWorkspace.getName(),
                newWorkspace.getName(), newWorkspace.getComment(), null, null);
        updateWorkspaceCommand.runSynchronously();
    }

    /**
     * This method Syncs the workspace based on the root path recursively.
     * This is a synchronous call so it should only be called on a background thread.
     */
    public static void syncWorkspace(final ServerContext context, final String rootPath) {
        final SyncCommand command = new SyncCommand(context, Collections.singletonList(rootPath), true);
        command.runSynchronously();
    }

    /**
     * This method undoes the list of local files passed in.
     * This is a synchronous call so it should only be called on a background thread.
     */
    public static List<String> undoLocalFiles(final ServerContext context, final List<String> files) {
        final UndoCommand command = new UndoCommand(context, files);
        return command.runSynchronously();
    }

    /**
     * Get the status for a single file
     *
     * @param context
     * @param file
     * @return
     */
    public static PendingChange getStatusForFile(final ServerContext context, final String file) {
        final Command<List<PendingChange>> command = new StatusCommand(context, file);
        final List<PendingChange> results = command.runSynchronously();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Renames a file
     *
     * @param context
     * @param oldName
     * @param newName
     */
    public static void renameFile(final ServerContext context, final String oldName, final String newName) {
        final Command<String> command = new RenameCommand(context, oldName, newName);
        command.runSynchronously();
    }

    /**
     * Resolves conflicts with the given resolution type
     *
     * @param context
     * @param conflicts
     * @param type
     * @return
     */
    public static List<Conflict> resolveConflictsByPath(final ServerContext context, final List<String> conflicts, final ResolveConflictsCommand.AutoResolveType type) {
        final Command<List<Conflict>> conflictsCommand = new ResolveConflictsCommand(context, conflicts, type);
        return conflictsCommand.runSynchronously();
    }

    public static List<Conflict> resolveConflictsByConflict(final ServerContext context, final List<Conflict> conflicts, final ResolveConflictsCommand.AutoResolveType type) {
        final List<String> conflictFiles = new ArrayList<String>();
        for (final Conflict conflict : conflicts) {
            conflictFiles.add(conflict.getLocalPath());
        }

        return resolveConflictsByPath(context, conflictFiles, type);
    }

    /**
     * Finds the conflicts under a given directory
     *
     * @param context
     * @param root
     * @return
     */
    public static List<Conflict> getConflicts(final ServerContext context, final String root) {
        final List<Conflict> conflicts = new ArrayList<Conflict>();

        final Command<ConflictResults> conflictsCommand = new FindConflictsCommand(context, root);
        final ConflictResults conflictResults = conflictsCommand.runSynchronously();

        for (final String contentConflict : conflictResults.getContentConflicts()) {
            conflicts.add(new Conflict(contentConflict, Conflict.ConflictType.CONTENT));
        }

        for (final String renameConflict : conflictResults.getRenameConflicts()) {
            final RenameConflict rename = findLocalRename(context, renameConflict, root, Conflict.ConflictType.RENAME);
            if (rename != null) {
                conflicts.add(rename);
            }
        }

        for (final String bothConflict : conflictResults.getBothConflicts()) {
            final RenameConflict rename = findLocalRename(context, bothConflict, root, Conflict.ConflictType.BOTH);
            if (rename != null) {
                conflicts.add(rename);
            }
        }

        return conflicts;
    }

    /**
     * For rename conflicts, find the old name and local name of the file by looking for the last rename entry in the
     * history. Look at the last 50 history entries first and if not found there look at all the history
     *
     * @param context
     * @param serverName
     * @param root
     * @param type
     * @return
     */
    private static RenameConflict findLocalRename(final ServerContext context, final String serverName,
                                                  final String root, final Conflict.ConflictType type) {
        final RenameConflict conflict = searchChangeSetForRename(context, serverName, root, type, 50);

        // return conflict if found, else do a search on all of the history (-1 will not add a stopAfter parameter to cmd)
        return conflict != null ? conflict : searchChangeSetForRename(context, serverName, root, type, -1);
    }

    private static RenameConflict searchChangeSetForRename(final ServerContext context, final String serverName,
                                                           final String root, final Conflict.ConflictType type,
                                                           final int stopAfter) {
        final List<ChangeSet> changeSets = CommandUtils.getHistoryCommand(context, serverName, StringUtils.EMPTY,
                stopAfter, false, StringUtils.EMPTY, true);

        // step through most current changesets to find the one that did the rename
        for (int index = 0; index < changeSets.size(); index++) {
            final ChangeSet changeSet = changeSets.get(index);
            if (doesChangeSetHaveChanges(changeSets, index) &&
                    changeSet.getChanges().get(0).getChangeTypes().contains(ServerStatusType.RENAME)) {
                // the entry after the rename contains the old name of the file
                if (doesChangeSetHaveChanges(changeSets, index + 1)) {
                    final String oldName = changeSets.get(index + 1).getChanges().get(0).getServerItem();

                    // parse local changes for the old file name to get the new local name
                    final Command<List<PendingChange>> command = new StatusCommand(context, root);
                    final List<PendingChange> results = command.runSynchronously();

                    for (final PendingChange change : results) {
                        if (SystemHelper.areFilePathsSame(change.getSourceItem(), oldName)) {
                            return new RenameConflict(change.getLocalItem(), serverName, oldName, type);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks that a changeset in the list contains a change
     *
     * @param changeSets
     * @param index
     * @return
     */
    private static boolean doesChangeSetHaveChanges(final List<ChangeSet> changeSets, final int index) {
        if (changeSets == null
                || index >= changeSets.size()
                || changeSets.get(index).getChanges() == null
                || changeSets.get(index).getChanges().isEmpty()) {
            return false;
        }

        return true;
    }
}
