/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import org.hibernate.persister.collection.spi.ImprovedCollectionPersister;
import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.collection.spi.PluralAttributeId;
import org.hibernate.persister.collection.spi.PluralAttributeIndex;
import org.hibernate.persister.collection.spi.PluralAttributeKey;
import org.hibernate.persister.common.spi.AbstractAttributeImpl;
import org.hibernate.sqm.domain.BasicType;
import org.hibernate.sqm.domain.ManagedType;
import org.hibernate.sqm.domain.PluralAttribute;
import org.hibernate.sqm.domain.Type;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeImpl extends AbstractAttributeImpl implements PluralAttribute {
	private final ImprovedCollectionPersister collectionPersister;

	private final CollectionClassification collectionClassification;
	private final PluralAttributeKey foreignKeyDescriptor;
	private final PluralAttributeId idDescriptor;
	private final PluralAttributeElement elementDescriptor;
	private final PluralAttributeIndex indexDescriptor;

	public PluralAttributeImpl(
			ManagedType declaringType,
			String name,
			ImprovedCollectionPersister collectionPersister,
			CollectionClassification collectionClassification,
			PluralAttributeKey foreignKeyDescriptor,
			PluralAttributeId idDescriptor,
			PluralAttributeElement elementDescriptor,
			PluralAttributeIndex indexDescriptor) {
		super( declaringType, name );
		this.collectionPersister = collectionPersister;
		this.collectionClassification = collectionClassification;
		this.foreignKeyDescriptor = foreignKeyDescriptor;
		this.idDescriptor = idDescriptor;
		this.elementDescriptor = elementDescriptor;
		this.indexDescriptor = indexDescriptor;
	}

	public ImprovedCollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	public CollectionClassification getCollectionClassification() {
		return collectionClassification;
	}

	public PluralAttributeKey getForeignKeyDescriptor() {
		return foreignKeyDescriptor;
	}

	public PluralAttributeId getIdDescriptor() {
		return idDescriptor;
	}

	public PluralAttributeElement getElementDescriptor() {
		return elementDescriptor;
	}

	public PluralAttributeIndex getIndexDescriptor() {
		return indexDescriptor;
	}

	@Override
	public ElementClassification getElementClassification() {
		return elementDescriptor.getElementClassification();
	}

	@Override
	public Type getElementType() {
		return elementDescriptor.getSqmType();
	}

	@Override
	public BasicType getCollectionIdType() {
		return idDescriptor.getSqmType();
	}

	@Override
	public Type getIndexType() {
		return indexDescriptor.getSqmType();
	}

	@Override
	public Type getBoundType() {
		return getElementType();
	}

	@Override
	public ManagedType asManagedType() {
		// todo : for now, just let the ClassCastException happen
		return (ManagedType) getBoundType();
	}
}
