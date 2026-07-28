// IAppManagerService.aidl
package com.casapreta.app.aidl;

/**
 * UserService interface executed in the Shizuku server process (uid 2000 / adb or uid 0 / root).
 *
 * The implementation lives in AppManagerService.kt and is launched via
 * Shizuku.bindUserService(...). Calls from the app process are transparently
 * forwarded to the privileged process by the Shizuku binder wrapper.
 */
interface IAppManagerService {

    /** Returns true if the package was successfully disabled (hidden from launcher). */
    boolean hidePackage(String packageName);

    /** Returns true if the package was successfully re-enabled. */
    boolean unhidePackage(String packageName);

    /** Returns true if the package is currently disabled. */
    boolean isPackageHidden(String packageName);

    /** Convenience helper for the settings screen: returns the running uid (0 = root, 2000 = adb). */
    int getPrivilegedUid();
}
