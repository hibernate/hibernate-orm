/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

/**
 * Details about the underlying database, as understood by a Dialect.
 *
 * @see org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo
 */
public interface DatabaseVersion {
	/**
	 * Constant used to indicate that no version is defined
	 */
	int NO_VERSION = -9999;

	/**
	 * Factory for DatabaseVersion based on major version (minor and micro set to zero)
	 */
	static DatabaseVersion make(Integer major) {
		return make( major, 0 );
	}

	/**
	 * Factory for DatabaseVersion based on major and minor version (micro set to zero)
	 */
	static DatabaseVersion make(Integer major, Integer minor) {
		return make( major, minor, 0 );
	}

	/**
	 * Factory for DatabaseVersion based on major, minor and micro
	 */
	static DatabaseVersion make(Integer major, Integer minor, Integer micro) {
		return new SimpleDatabaseVersion( major, minor, micro );
	}

	/**
	 * Obtain access to the database major version, as returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion()} for the target database.
	 *
	 * @return The database major version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseMajorVersion()
	 */
	int getDatabaseMajorVersion();

	/**
	 * Obtain access to the database minor version, as returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion()} for the target database.
	 *
	 * @return The database minor version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseMinorVersion()
	 */
	int getDatabaseMinorVersion();

	/**
	 * Obtain access to the database minor version, as returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion()} for the target database.
	 *
	 * @return The database minor version, or {@value #NO_VERSION} to indicate "no version information"
	 * @see java.sql.DatabaseMetaData#getDatabaseMinorVersion()
	 */
	default int getDatabaseMicroVersion() {
		return 0;
	}

	/**
	 * Make a simple copy of this version object
	 */
	default DatabaseVersion makeCopy() {
		return new SimpleDatabaseVersion( this );
	}

	/**
	 * Make a copy of this version object, possibly converting {@link #NO_VERSION}
	 * to zero
	 */
	default DatabaseVersion makeCopy(boolean noVersionAsZero) {
		return new SimpleDatabaseVersion( this, noVersionAsZero );
	}

	/**
	 * Determine if the versions are the same/equal.
	 */
	default boolean isSame(DatabaseVersion other) {
		return isSame( other.getDatabaseMajorVersion(), other.getDatabaseMinorVersion(), other.getDatabaseMicroVersion() );
	}

	/**
	 * Determine if this version matches the passed one.
	 */
	default boolean isSame(int otherMajor) {
		return getDatabaseMajorVersion() == otherMajor;
	}

	/**
	 * Determine if this version matches the passed one.
	 */
	default boolean isSame(int otherMajor, int otherMinor) {
		return isSame( otherMajor ) && getDatabaseMinorVersion() == otherMinor;
	}

	/**
	 * Determine if this version matches the passed one.
	 */
	default boolean isSame(int otherMajor, int otherMinor, int otherMicro) {
		return isSame( otherMajor, otherMinor ) && getDatabaseMicroVersion() == otherMicro;
	}

	/**
	 * {@link #isSame} or {@link #isAfter}
	 */
	default boolean isSince(DatabaseVersion other) {
		return isSince( other.getDatabaseMajorVersion(), other.getDatabaseMinorVersion() );
	}

	/**
	 * {@link #isSame} or {@link #isAfter}
	 */
	default boolean isSince(Integer otherMajor, Integer otherMinor) {
		return isSince(
				(int) otherMajor,
				otherMinor == null ? NO_VERSION : otherMinor
		);
	}

	/**
	 * {@link #isSame} or {@link #isAfter}
	 */
	default boolean isSince(int otherMajor) {
		final int major = getDatabaseMajorVersion();

		return major >= otherMajor;
	}

	/**
	 * {@link #isSame} or {@link #isAfter}
	 */
	default boolean isSince(int otherMajor, int otherMinor) {
		final int major = getDatabaseMajorVersion();
		final int minor = getDatabaseMinorVersion();

		return major > otherMajor
				|| ( major == otherMajor && minor >= otherMinor );
	}

	/**
	 * {@link #isSame} or {@link #isAfter}
	 */
	default boolean isSince(int otherMajor, int otherMinor, int otherMicro) {
		final int major = getDatabaseMajorVersion();
		final int minor = getDatabaseMinorVersion();
		final int micro = getDatabaseMicroVersion();

		return major > otherMajor
				|| ( major == otherMajor && minor > otherMinor )
				|| ( major == otherMajor && minor == otherMinor && micro >= otherMicro );
	}

	/**
	 * Determine whether this version comes after the passed one
	 */
	default boolean isAfter(DatabaseVersion other) {
		return isAfter( other.getDatabaseMajorVersion(), other.getDatabaseMinorVersion() );
	}

	/**
	 * Determine whether this version after the passed one
	 */
	default boolean isAfter(Integer major, Integer minor) {
		return isAfter( (int) major, minor == null ? NO_VERSION : minor );
	}

	/**
	 * Determine whether this version after the passed one
	 */
	default boolean isAfter(int major) {
		return getDatabaseMajorVersion() > major;
	}

	/**
	 * Determine whether this version after the passed one
	 */
	default boolean isAfter(int major, int minor) {
		return getDatabaseMajorVersion() > major
				|| ( getDatabaseMajorVersion() == major && getDatabaseMinorVersion() > minor );
	}

	/**
	 * Determine whether this version after the passed one
	 */
	default boolean isAfter(int otherMajor, int otherMinor, int otherMicro) {
		final int major = getDatabaseMajorVersion();
		final int minor = getDatabaseMinorVersion();
		final int micro = getDatabaseMicroVersion();

		return major > otherMajor
				|| ( major == otherMajor && minor > otherMinor )
				|| ( major == otherMajor && minor == otherMinor && otherMicro > micro );
	}

	/**
	 * Determine whether this version comes before the passed one
	 */
	default boolean isBefore(DatabaseVersion other) {
		return isBefore( other.getDatabaseMajorVersion(), other.getDatabaseMinorVersion() );
	}

	/**
	 * Determine whether this version before the passed one
	 */
	default boolean isBefore(int major, int minor) {
		return ! isSince( major, minor );
	}

	/**
	 * Determine whether this version before the passed one
	 */
	default boolean isBefore(int major) {
		return ! isSince( major );
	}

	/**
	 * Determine whether this version before the passed one
	 */
	default boolean isBefore(Integer major, Integer minor) {
		return isBefore( (int) major, minor == null ? NO_VERSION : minor );
	}

	/**
	 * Determine whether this version before the passed one
	 */
	default boolean isBefore(int otherMajor, int otherMinor, int otherMicro) {
		return ! isSince( otherMajor, otherMinor, otherMicro );
	}

	default int getMajor() {
		return getDatabaseMajorVersion();
	}

	default int getMinor() {
		return getDatabaseMinorVersion();
	}

	default int getMicro() {
		return getDatabaseMicroVersion();
	}
}
