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
import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Chris Cranford
 */
public class ArrayInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = ArrayInitializer.class.getSimpleName();

	private final DomainResultAssembler<Integer> listIndexAssembler;
	private final DomainResultAssembler<?> elementAssembler;

	private final int indexBase;

	public ArrayInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping arrayDescriptor,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyAssembler,
			DomainResultAssembler<?> collectionValueKeyAssembler,
			DomainResultAssembler<Integer> listIndexAssembler,
			DomainResultAssembler<?> elementAssembler) {
		super(
				navigablePath,
				arrayDescriptor,
				parentAccess,
				lockMode,
				collectionKeyAssembler,
				collectionValueKeyAssembler
		);
		this.listIndexAssembler = listIndexAssembler;
		this.elementAssembler = elementAssembler;
		this.indexBase = getCollectionAttributeMapping().getIndexMetadata().getListIndexBase();
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	public PersistentArrayHolder<?> getCollectionInstance() {
		return (PersistentArrayHolder<?>) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState) {
		final Integer indexValue = listIndexAssembler.assemble( rowProcessingState );
		if ( indexValue == null ) {
			throw new HibernateException( "Illegal null value for array index encountered while reading: "
					+ getCollectionAttributeMapping().getNavigableRole() );
		}
		final Object element = elementAssembler.assemble( rowProcessingState );
		if ( element == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		int index = indexValue;

		if ( indexBase != 0 ) {
			index -= indexBase;
		}

		for ( int i = loadingState.size(); i <= index; ++i ) {
			loadingState.add( i, null );
		}

		loadingState.set( index, element );
	}

	@Override
	public String toString() {
		return "ArrayInitializer{" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
