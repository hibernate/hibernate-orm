/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.internal.PersistentArrayHolder;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Chris Cranford
 */
public class ArrayInitializer implements CollectionInitializer {
	private final CollectionPersister arrayDescriptor;
	private final NavigablePath navigablePath;
//	private final DomainResultAssembler listIndexAssembler;
//	private final DomainResultAssembler elementAssembler;

//	public ArrayInitializer(
//			CollectionPersister arrayDescriptor,
//			FetchParentAccess parentAccess,
//			NavigablePath navigablePath,
//			boolean selected,
//			LockMode lockMode,
//			DomainResultAssembler keyContainerAssembler,
//			DomainResultAssembler keyCollectionAssembler,
//			DomainResultAssembler listIndexAssembler,
//			DomainResultAssembler elementAssembler) {
//		super( arrayDescriptor, parentAccess, navigablePath, selected, lockMode, keyContainerAssembler, keyCollectionAssembler );
//		this.listIndexAssembler = listIndexAssembler;
//		this.elementAssembler = elementAssembler;
//	}

	public ArrayInitializer(
			CollectionPersister arrayDescriptor,
			NavigablePath navigablePath) {
		this.arrayDescriptor = arrayDescriptor;
		this.navigablePath = navigablePath;
	}

	@Override
	public CollectionPersister getInitializingCollectionDescriptor() {
		return arrayDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {

	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {

	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {

	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {

	}

	@Override
	public PersistentArrayHolder getCollectionInstance() {
		throw new NotYetImplementedFor6Exception( getClass() );
		//return (PersistentArrayHolder) super.getCollectionInstance();
	}

//	@Override
//	protected void readCollectionRow(RowProcessingState rowProcessingState) {
//		int index = (int) listIndexAssembler.assemble( rowProcessingState );
//		if ( getCollectionDescriptor().getIndexDescriptor().getBaseIndex() != 0 ) {
//			index -= getCollectionDescriptor().getIndexDescriptor().getBaseIndex();
//		}
//		getCollectionInstance().load( index, elementAssembler.assemble( rowProcessingState ) );
//	}
//
//	@Override
//	public String toString() {
//		return "ArrayInitializer{" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
//	}
}
