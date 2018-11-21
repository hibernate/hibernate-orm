/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.support.domains.DomainModel;

/**
 * @author Steve Ebersole
 */
public class GambitDomainModel implements DomainModel {
	public static final GambitDomainModel INSTANCE = new GambitDomainModel();

	private static final Class[] CLASSES = new Class[] {
			BasicEntity.class,
			Component.class,
			EmbeddedIdEntity.class,
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
			EntityWithOneToMany.class,
			EntityWithOneToOne.class,
			EntityWithOneToOneJoinTable.class,
			EntityWithOneToOneSharingPrimaryKey.class,
			Shirt.class,
			SimpleEntity.class
	};

	@Override
	public void applyDomainModel(MetadataSources sources) {
		for ( Class domainClass : CLASSES ) {
			sources.addAnnotatedClass( domainClass );
		}
	}
}
