/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
