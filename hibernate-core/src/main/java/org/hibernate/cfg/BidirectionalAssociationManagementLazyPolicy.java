/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import java.util.Locale;

import org.hibernate.HibernateException;

/// Policy for runtime bidirectional association management when an inverse
/// attribute or collection is not already loaded during post-flush repair.
///
/// @see PersistenceSettings#BIDIRECTIONALITY_MANAGEMENT_LAZY_POLICY
///
/// @since 8.0
/// @author Steve Ebersole
public enum BidirectionalAssociationManagementLazyPolicy {
	/// Skip repair for unloaded inverse attributes and collections.
	///
	/// This policy never initializes lazy inverse state during post-flush
	/// bidirectional association management.
	SKIP,

	/// Skip repair for unloaded inverse attributes and collections (like [#SKIP]), and log a
	/// warning once per association role during each flush.
	WARN,

	/// Initialize unloaded inverse attributes and collections of already-managed
	/// entities when they are needed to repair the in-memory object graph.
	INITIALIZE;

	public static BidirectionalAssociationManagementLazyPolicy interpret(Object value) {
		if ( value == null ) {
			return WARN;
		}
		if ( value instanceof BidirectionalAssociationManagementLazyPolicy policy ) {
			return policy;
		}

		final String name = value.toString().trim().replace( '-', '_' ).toUpperCase( Locale.ROOT );
		for ( BidirectionalAssociationManagementLazyPolicy policy : values() ) {
			if ( policy.name().equals( name ) ) {
				return policy;
			}
		}

		throw new HibernateException( "Unrecognized bidirectional association management lazy policy: " + value );
	}
}
