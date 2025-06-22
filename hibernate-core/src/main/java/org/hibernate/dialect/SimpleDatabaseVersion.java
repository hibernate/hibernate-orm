/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.util.Objects;

/**
 * Simple version of DatabaseVersion
 */
public class SimpleDatabaseVersion implements DatabaseVersion {
	public static final SimpleDatabaseVersion ZERO_VERSION = new SimpleDatabaseVersion( 0, 0 );

	private final int major;
	private final int minor;
	private final int micro;

	public SimpleDatabaseVersion(DatabaseVersion copySource) {
		this( copySource, true );
	}

	public SimpleDatabaseVersion(DatabaseVersion version, boolean noVersionAsZero) {
		this.major = version.getDatabaseMajorVersion();

		if ( version.getDatabaseMinorVersion() == NO_VERSION ) {
			this.minor = noVersionAsZero ? 0 : NO_VERSION;
		}
		else {
			this.minor = version.getDatabaseMinorVersion();
		}

		if ( version.getDatabaseMicroVersion() == NO_VERSION ) {
			this.micro = noVersionAsZero ? 0 : NO_VERSION;
		}
		else {
			this.micro = version.getDatabaseMicroVersion();
		}
	}

	public SimpleDatabaseVersion(int major, int minor) {
		this( major, minor, 0 );
	}

	public SimpleDatabaseVersion(int major, int minor, int micro) {
		this.major = major;
		this.minor = minor;
		this.micro = micro;
	}

	public SimpleDatabaseVersion(Integer major, Integer minor) {
		if ( major == null ) {
			throw new IllegalArgumentException( "Major version can not be null" );
		}

		this.major = major;
		this.minor = minor == null ? NO_VERSION : minor;
		this.micro = 0;
	}

	@Override
	public int getDatabaseMajorVersion() {
		return major;
	}

	@Override
	public int getDatabaseMinorVersion() {
		return minor;
	}

	@Override
	public int getDatabaseMicroVersion() {
		return micro;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getMicro() {
		return micro;
	}

	@Override
	public String toString() {
		StringBuilder version = new StringBuilder();
		if ( major != NO_VERSION ) {
			version.append( major );
		}
		if ( minor != NO_VERSION ) {
			version.append( "." );
			version.append( minor );
			if ( micro > 0 ) {
				version.append( "." );
				version.append( micro );
			}
		}
		return version.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		SimpleDatabaseVersion that = (SimpleDatabaseVersion) o;
		return major == that.major && minor == that.minor && micro == that.micro;
	}

	@Override
	public int hashCode() {
		return Objects.hash( major, minor, micro );
	}
}
