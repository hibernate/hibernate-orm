/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm;

import java.io.Serializable;
import java.util.Objects;

/**
 * Major/family version component pair
 *
 * @author Steve Ebersole
 */
public class ReleaseFamilyIdentifier implements Comparable<ReleaseFamilyIdentifier>, Serializable {
	private final int majorVersion;
	private final int familyVersion;

	public ReleaseFamilyIdentifier(int majorVersion, int familyVersion) {
		this.majorVersion = majorVersion;
		this.familyVersion = familyVersion;
	}

	/**
	 * The {@code 5} in {@code 5.6}, or the {@code 6} in {@code 6.0}
	 */
	public int getMajorVersion() {
		return majorVersion;
	}

	/**
	 * The {@code 6} in {@code 5.6}, or the {@code 0} in {@code 6.0}
	 */
	public int getFamilyVersion() {
		return familyVersion;
	}

	@Override
	public String toString() {
		return "ReleaseFamilyIdentifier( " + majorVersion + " . " + familyVersion + " )";
	}

	public String toExternalForm() {
		return majorVersion + "." + familyVersion;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ReleaseFamilyIdentifier that = (ReleaseFamilyIdentifier) o;
		return majorVersion == that.majorVersion && familyVersion == that.familyVersion;
	}

	@Override
	public int hashCode() {
		return Objects.hash( majorVersion, familyVersion );
	}

	public boolean newerThan(ReleaseFamilyIdentifier other) {
		return compareTo( other ) > 0;
	}

	public boolean olderThan(ReleaseFamilyIdentifier other) {
		return compareTo( other ) < 0;
	}

	public static ReleaseFamilyIdentifier parse(String version) {
		assert version != null && !version.isEmpty();

		final int delimiter = version.indexOf( '.' );
		assert delimiter > 0;

		final int secondDelimiter = version.indexOf( '.', delimiter + 1 );
		assert secondDelimiter < 0 || secondDelimiter > delimiter;

		final String majorString = version.substring( 0, delimiter );
		final String familyString;
		if ( secondDelimiter < 0 ) {
			familyString = version.substring( delimiter + 1 );
		}
		else {
			familyString = version.substring( delimiter + 1, secondDelimiter );
		}

		final int major = Integer.parseInt( majorString );
		final int family = Integer.parseInt( familyString );

		return new ReleaseFamilyIdentifier( major, family );
	}

	@Override
	public int compareTo(ReleaseFamilyIdentifier other) {
		if ( this == other ) {
			return 0;
		}

		if ( this.majorVersion < other.majorVersion ) {
			return -1;
		}

		if ( this.majorVersion > other.majorVersion ) {
			return 1;
		}

		return Integer.compare( this.familyVersion, other.familyVersion );
	}
}
