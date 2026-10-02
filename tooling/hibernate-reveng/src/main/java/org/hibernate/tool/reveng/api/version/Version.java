package org.hibernate.tool.reveng.api.version;

public interface Version {

	static String versionString() {
		return org.hibernate.Version.getVersionString();
	}

}
