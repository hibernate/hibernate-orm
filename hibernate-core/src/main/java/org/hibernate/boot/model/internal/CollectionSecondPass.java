/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Value;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Collection second pass
 *
 * @author Emmanuel Bernard
 */
public abstract class CollectionSecondPass implements SecondPass {

	private static final CoreMessageLogger LOG = messageLogger( CollectionSecondPass.class);

	private final Collection collection;

	public CollectionSecondPass(Collection collection) {
		this.collection = collection;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses)
			throws MappingException {
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Second pass for collection: " + collection.getRole() );
		}

		secondPass( persistentClasses );
		collection.createAllKeys();

		if ( LOG.isTraceEnabled() ) {
			String msg = "Mapped collection key: " + columns( collection.getKey() );
			if ( collection.isIndexed() ) {
				msg += ", index: " + columns( ( (IndexedCollection) collection ).getIndex() );
			}
			if ( collection.isOneToMany() ) {
				msg += ", one-to-many: "
					+ ( (OneToMany) collection.getElement() ).getReferencedEntityName();
			}
			else {
				msg += ", element: " + columns( collection.getElement() );
			}
			LOG.trace( msg );
		}
	}

	abstract public void secondPass(Map<String, PersistentClass> persistentClasses) throws MappingException;

	private static String columns(Value val) {
		final var columns = new StringBuilder();
		for ( var selectable : val.getSelectables() ) {
			if ( !columns.isEmpty() ) {
				columns.append( ", " );
			}
			columns.append( selectable.getText() );
		}
		return columns.toString();
	}
}
