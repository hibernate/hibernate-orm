/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.log;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;

/**
 * Helper for logging collection, entity and embeddable information.  Uses path collapsing
 * for readability
 *
 * @author Steve Ebersole
 */
public class LoggingHelper {
	public static final String NULL = "<null>";
	public static final String UNREFERENCED = "<unreferenced>";

	public static String toLoggableString(NavigableRole role) {
		if ( role == null ) {
			return UNREFERENCED;
		}

		return role.getFullPath();
	}

	public static String toLoggableString(NavigablePath path) {
		assert path != null;

		return path.getFullPath();
	}

	public static String toLoggableString(NavigableRole role, Object key) {
		if ( role == null ) {
			return UNREFERENCED;
		}

		return toLoggableString( toLoggableString( role ), key );
	}

	public static String toLoggableString(NavigablePath path, Object key) {
		assert path != null;
		return toLoggableString( toLoggableString( path ), key );
	}

	public static String toLoggableString(CollectionKey collectionKey) {
		return toLoggableString( collectionKey.getRole(), collectionKey.getKey() );
	}

	public static String toLoggableString(EntityKey entityKey) {
		return toLoggableString( StringHelper.collapse( entityKey.getEntityName() ), entityKey.getIdentifierValue() );
	}

	private static String toLoggableString(String roleOrPath, Object key) {
		assert roleOrPath != null;

		StringBuilder buffer = new StringBuilder();

		buffer.append( roleOrPath );
		buffer.append( '#' );

		if ( key == null ) {
			buffer.append( NULL );
		}
		else {
			buffer.append( key );
		}

		return buffer.toString();
	}

	public static String toLoggableString(PersistentCollection<?> collectionInstance) {
		if ( collectionInstance == null ) {
			return NULL;
		}

		return toLoggableString(
				collectionInstance.getRole(),
				collectionInstance.getKey()
		);
	}
}
