// Interface of the Shizuku user service that executes shell commands with
// shell-uid privileges. See ShellUserService for the implementation.
package dev.xitee.sleeptimer.core.service.shizuku;

import dev.xitee.sleeptimer.core.service.media.PlayingTrack;

interface IShellUserService {

    void destroy() = 16777114; // Destroy method defined by the Shizuku server

    // Runs the command and returns its exit code; negative values signal
    // timeout (-2) or failure to launch (-1).
    int exec(in String[] command, long timeoutMillis) = 1;

    // The playing song, or null when nothing is playing or its length or position
    // is unknown.
    PlayingTrack playingTrack() = 2;
}
