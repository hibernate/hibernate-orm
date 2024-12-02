package org.hibernate.tool.api.version;

public interface Version {

	/**
	 * @deprecated Use {@link #versionString()} instead.
	 */
	@Deprecated
	final static String CURRENT_VERSION = versionString();

	static String versionString() {
		// This implementation is replaced during the build with another one that returns the correct value.
		return "UNKNOWN";
	}
}
