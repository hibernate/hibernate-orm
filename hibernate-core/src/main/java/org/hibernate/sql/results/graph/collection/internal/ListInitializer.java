/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentList;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * CollectionInitializer for PersistentList loading
 * 
 * @author Steve Ebersole
 */
public class ListInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = ListInitializer.class.getSimpleName();

	private final DomainResultAssembler<Integer> listIndexAssembler;
	private final DomainResultAssembler<?> elementAssembler;

	private final int listIndexBase;

	public ListInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyAssembler,
			DomainResultAssembler<?> collectionValueKeyAssembler,
			DomainResultAssembler<Integer> listIndexAssembler,
			DomainResultAssembler<?> elementAssembler) {
		super(
				navigablePath,
				attributeMapping,
				parentAccess,
				lockMode,
				collectionKeyAssembler,
				collectionValueKeyAssembler
		);
		this.listIndexAssembler = listIndexAssembler;
		this.elementAssembler = elementAssembler;
		this.listIndexBase = attributeMapping.getIndexMetadata().getListIndexBase();
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	public PersistentList<?> getCollectionInstance() {
		return (PersistentList<?>) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState) {
		final Integer indexValue = listIndexAssembler.assemble( rowProcessingState );
		if ( indexValue == null ) {
			throw new HibernateException( "Illegal null value for list index encountered while reading: "
					+ getCollectionAttributeMapping().getNavigableRole() );
		}
		int index = indexValue;

		if ( listIndexBase != 0 ) {
			index -= listIndexBase;
		}

		for ( int i = loadingState.size(); i <= index; ++i ) {
			loadingState.add( i, null );
		}

		loadingState.set( index, elementAssembler.assemble( rowProcessingState ) );
	}

	@Override
	public String toString() {
		return "ListInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
