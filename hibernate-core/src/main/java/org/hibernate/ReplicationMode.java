/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.type.BasicType;

/**
 * Represents a replication strategy used by
 * {@link Session#replicate(Object, ReplicationMode)}.
 *
 * @author Gavin King
 *
 * @see Session#replicate(Object, ReplicationMode)
 *
 * @deprecated since {@link Session#replicate(Object, ReplicationMode)} is deprecated
 */
@Deprecated(since="6.2")
public enum ReplicationMode {
	/**
	 * Throw an exception when a row already exists.
	 */
	EXCEPTION {
		@Override
		public <T> boolean shouldOverwriteCurrentVersion(
				T currentVersion, T newVersion,
				BasicType<T> versionType) {
			throw new AssertionFailure( "should not be called" );
		}
	},
	/**
	 * Ignore replicated entities when a row already exists.
	 */
	IGNORE {
		@Override
		public <T> boolean shouldOverwriteCurrentVersion(
				T currentVersion, T newVersion,
				BasicType<T> versionType) {
			return false;
		}
	},
	/**
	 * Overwrite existing rows when a row already exists.
	 */
	OVERWRITE {
		@Override
		public <T> boolean shouldOverwriteCurrentVersion(
				T currentVersion, T newVersion,
				BasicType<T> versionType) {
			return true;
		}
	},
	/**
	 * When a row already exists, choose the latest version.
	 */
	LATEST_VERSION {
		@Override
		public <T> boolean shouldOverwriteCurrentVersion(
				T currentVersion, T newVersion,
				BasicType<T> versionType) {
			// always overwrite non-versioned data (because we don't know which is newer)
			return versionType == null
				|| versionType.getJavaTypeDescriptor().getComparator()
					.compare( currentVersion, newVersion ) <= 0;
		}
	};

	/**
	 * Determine whether the mode dictates that the data being replicated should overwrite the data found.
	 *
	 * @param currentVersion The version currently on the target database table.
	 * @param newVersion The replicating version
	 * @param versionType The version type
	 *
	 * @return {@code true} indicates the data should be overwritten; {@code false} indicates it should not.
	 */
	public abstract <T> boolean shouldOverwriteCurrentVersion(
			T currentVersion, T newVersion,
			BasicType<T> versionType);
}
