/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;

/**
 * @author Steve Ebersole
 */
public class GambitDomainModel extends AbstractDomainModelDescriptor {
	public static final GambitDomainModel INSTANCE = new GambitDomainModel();

	public GambitDomainModel() {
		super(
				BasicEntity.class,
				VersionedEntity.class,
				Component.class,
				EmbeddedIdEntity.class,
				EntityOfArrays.class,
				EntityOfBasics.class,
				EntityOfComposites.class,
				EntityOfDynamicComponent.class,
				EntityOfLists.class,
				EntityOfMaps.class,
				EntityOfSets.class,
				EntityWithLazyManyToOneSelfReference.class,
				EntityWithLazyOneToOne.class,
				EntityWithManyToOneJoinTable.class,
				EntityWithManyToOneSelfReference.class,
				EntityWithNonIdAttributeNamedId.class,
				EntityWithAggregateId.class,
				EntityWithOneToMany.class,
				EntityWithOneToOne.class,
				EntityWithOneToOneJoinTable.class,
				EntityWithOneToOneSharingPrimaryKey.class,
				Shirt.class,
				SimpleEntity.class
		);
	}
}
