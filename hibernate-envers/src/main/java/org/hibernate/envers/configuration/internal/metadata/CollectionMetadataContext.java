/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.type.BagType;
import org.hibernate.type.ListType;
import org.hibernate.type.MapType;
import org.hibernate.type.SetType;
import org.hibernate.type.Type;

/**
 * Metadata building context used for collections to hold per-collection state.
 *
 * @author Chris Cranford
 */
public interface CollectionMetadataContext {

	EntityMappingData getEntityMappingData();

	Collection getCollection();

	CompositeMapperBuilder getMapperBuilder();

	String getReferencedEntityName();

	String getReferencingEntityName();

	EntityConfiguration getReferencingEntityConfiguration();

	PropertyAuditingData getPropertyAuditingData();

	default String getPropertyName() {
		return getPropertyAuditingData().getName();
	}

	default boolean isOneToManyAttached() {
		Type type = getCollection().getType();
		return type instanceof BagType
				|| type instanceof SetType
				|| type instanceof MapType
				|| type instanceof ListType;
	}

	default boolean isInverseOneToMany() {
		return getCollection().getElement() instanceof OneToMany
				&& getCollection().isInverse();
	}

	default boolean isOwningManyToOneWithBidrectionalJoinTable() {
		return getCollection().getElement() instanceof ManyToOne
				&& getPropertyAuditingData().hasRelationMappedBy();
	}

	default boolean isFakeOneToManyBidirectional() {
		return getCollection().getElement() instanceof OneToMany
				&& getPropertyAuditingData().hasAuditedMappedBy();
	}

	default boolean isMiddleTableCollection() {
		return !( isOneToManyAttached()
				&& ( isInverseOneToMany()
					|| isFakeOneToManyBidirectional()
					|| isOwningManyToOneWithBidrectionalJoinTable() ) );
	}

	default boolean isOneToManySingleTableSubclass() {
		if ( getCollection().getElement() instanceof OneToMany ) {
			return ( (OneToMany) getCollection().getElement() ).getAssociatedClass() instanceof SingleTableSubclass;
		}
		return false;
	}
}
