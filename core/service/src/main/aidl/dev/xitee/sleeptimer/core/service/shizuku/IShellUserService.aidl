// Interface of the Shizuku user service that executes shell commands with
// shell-uid privileges. See ShellUserService for the implementation.
package dev.xitee.sleeptimer.core.service.shizuku;

interface IShellUserService {

    void destroy() = 16777114; // Destroy method defined by the Shizuku server

    // Runs the command and returns its exit code; negative values signal
    // timeout (-2) or failure to launch (-1).
    int exec(in String[] command, long timeoutMillis) = 1;
}
