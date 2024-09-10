package org.wildfly.extras.creaper.core.offline;

public class NameSpaceVersion {
    private int major;
    private int minor;

    public NameSpaceVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Detect whether current version is less then another version.
     * @param comparison Second version for comparison.
     * @return True or false based on the check.
     */
    public boolean lessThen(NameSpaceVersion comparison) {
        if (this.major < comparison.major) {
            return true;
        } else if (this.major == comparison.major) {
            if (this.minor < comparison.minor) {
                return true;
            }
        }
        return false;
    }
}
