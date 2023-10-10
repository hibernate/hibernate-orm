/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Represents an immediate initialization of some sort (join, select, batch, sub-select)
 * of a persistent Map valued attribute.
 *
 * @see DelayedCollectionInitializer
 *
 * @author Steve Ebersole
 */
public class MapInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = MapInitializer.class.getSimpleName();

	private final DomainResultAssembler<?> mapKeyAssembler;
	private final DomainResultAssembler<?> mapValueAssembler;

	public MapInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyAssembler,
			DomainResultAssembler<?> collectionValueKeyAssembler,
			DomainResultAssembler<?> mapKeyAssembler,
			DomainResultAssembler<?> mapValueAssembler) {
		super( navigablePath, attributeMapping, parentAccess, lockMode, collectionKeyAssembler, collectionValueKeyAssembler );
		this.mapKeyAssembler = mapKeyAssembler;
		this.mapValueAssembler = mapValueAssembler;
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	public PersistentMap<?, ?> getCollectionInstance() {
		return (PersistentMap<?, ?>) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState) {
		final Object key = mapKeyAssembler.assemble( rowProcessingState );
		if ( key == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		final Object value = mapValueAssembler.assemble( rowProcessingState );
		if ( value == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		loadingState.add( new Object[] { key, value } );
	}

	@Override
	public String toString() {
		return "MapInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
