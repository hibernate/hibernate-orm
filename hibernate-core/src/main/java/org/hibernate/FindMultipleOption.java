/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;

import java.util.List;

/// Simple marker interface for FindOptions which can be applied to multiple id loading.
///
/// @see org.hibernate.Session#findMultiple(Class, List, FindOption...)
/// @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
///
/// @since 7.2
public interface FindMultipleOption extends FindOption {
	/// Indicates whether the result list should be ordered relative to the
	/// position of the identifier list.  E.g.
	/// ```java
	/// var results = session.findMultiple(
	///     Person.class,
	///     List.of(1,2,3,2),
	///     ORDERED
	/// );
	/// assert results.get(0).getId() == 1;
	/// assert results.get(1).getId() == 2;
	/// assert results.get(2).getId() == 3;
	/// assert results.get(3).getId() == 2;
	/// ```
	///
	/// The default is [#ORDERED].
	///
	/// @since 7.2
	enum OrderingMode implements FindMultipleOption {
		/// The default.  The result list is ordered relative to the
		/// position of the identifiers list.
		///
		/// @see RemovalsMode
		ORDERED,

		/// The result list may be in any order.
		UNORDERED
	}

	/// Indicates whether the persistence context should be checked for entities
	/// matching the identifiers to be loaded -
	///   - Entities which are in a managed state are not reloaded from the database.
	///     		Those identifiers are removed from the SQL restriction sent to the database.
	///   - Entities which are in a removed state are {@linkplain RemovalsMode#REPLACE replaced with null}
	///     		from the result by default, but can be {@linkplain RemovalsMode#INCLUDE included} if desired.
	///
	///
	/// The default is [#DISABLED].
	///
	/// @since 7.2
	enum SessionCheckMode implements FindMultipleOption {
		/// The persistence context will be checked.  Identifiers for entities already contained
		/// in the persistence context will not be sent to the database for loading.  If the
		/// entity is marked for removal in the persistence context, whether it is returned
		/// is controlled by {@linkplain RemovalsMode}.
		///
		/// @see RemovalsMode
		ENABLED,

		/// The default.  All identifiers to be loaded will be read from the database and returned.
		DISABLED
	}

	/// When {@linkplain SessionCheckMode} is enabled, this option controls how
	/// to handle entities which are already contained by the persistence context
	/// but which are in a removed state (marked for removal, but not yet flushed).
	///
	/// The default is [#REPLACE].
	///
	/// @since 7.2
	enum RemovalsMode implements FindMultipleOption {
		/// Removed entities are included in the load result.
		INCLUDE,

		/// The default.  Removed entities are replaced with `null` in the load result.
		REPLACE,

		/// Removed entities are excluded from the load result.
		///
		/// This option is incompatible with [OrderingMode#ORDERED].
		/// It must be used in conjunction with [OrderingMode#UNORDERED]
		/// and [SessionCheckMode#ENABLED].
		///
		/// @since 7.3
		EXCLUDE
	}
}
