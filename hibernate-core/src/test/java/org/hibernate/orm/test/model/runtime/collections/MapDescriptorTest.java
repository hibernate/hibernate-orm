/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.model.runtime.collections;

import org.hibernate.boot.MetadataSources;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.Component;
import org.hibernate.orm.test.support.domains.gambit.EntityOfMaps;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class MapDescriptorTest extends SessionFactoryBasedFunctionalTest {
	private EntityTypeDescriptor entityDescriptor;

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( EntityOfMaps.class );
	}

	@BeforeAll
	public void findEntityDescriptor() {
		entityDescriptor = sessionFactory().getMetamodel().findEntityDescriptor( EntityOfMaps.class );
	}

	@Test
	public void testMapComponents() {
		testMapComponents(
				(PluralPersistentAttribute) entityDescriptor.findPersistentAttribute( "basicToBasicMap" ),
				CollectionIndex.IndexClassification.BASIC,
				String.class,
				CollectionElement.ElementClassification.BASIC,
				String.class
		);

		testMapComponents(
				(PluralPersistentAttribute) entityDescriptor.findPersistentAttribute( "basicToComponentMap" ),
				CollectionIndex.IndexClassification.BASIC,
				String.class,
				CollectionElement.ElementClassification.EMBEDDABLE,
				Component.class
		);

		testMapComponents(
				(PluralPersistentAttribute) entityDescriptor.findPersistentAttribute( "componentToBasicMap" ),
				CollectionIndex.IndexClassification.EMBEDDABLE,
				Component.class,
				CollectionElement.ElementClassification.BASIC,
				String.class
		);

		testMapComponents(
				(PluralPersistentAttribute) entityDescriptor.findPersistentAttribute( "basicToOneToMany" ),
				CollectionIndex.IndexClassification.BASIC,
				String.class,
				CollectionElement.ElementClassification.ONE_TO_MANY,
				EntityOfMaps.class
		);

		testMapComponents(
				(PluralPersistentAttribute) entityDescriptor.findPersistentAttribute( "basicToManyToMany" ),
				CollectionIndex.IndexClassification.BASIC,
				String.class,
				CollectionElement.ElementClassification.MANY_TO_MANY,
				EntityOfMaps.class
		);
	}

	private void testMapComponents(
			PluralPersistentAttribute mapAttribute,
			CollectionIndex.IndexClassification indexClassification,
			Class indexJavaType,
			CollectionElement.ElementClassification elementClassification,
			Class elementJavaType) {
		assertThat(
				mapAttribute.getPersistentCollectionDescriptor().getIndexDescriptor().getClassification(),
				is( indexClassification )
		);

		assertThat(
				mapAttribute.getPersistentCollectionDescriptor().getIndexDescriptor().getJavaTypeDescriptor().getJavaType(),
				is( equalTo( indexJavaType ) )
		);

		assertThat(
				mapAttribute.getPersistentCollectionDescriptor().getElementDescriptor().getClassification(),
				is( elementClassification )
		);

		assertThat(
				mapAttribute.getPersistentCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getJavaType(),
				is( equalTo( elementJavaType ) )
		);
	}
}
