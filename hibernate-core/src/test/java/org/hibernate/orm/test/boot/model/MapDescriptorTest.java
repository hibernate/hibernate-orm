/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.boot.model;

import org.hibernate.boot.MetadataSources;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityOfMaps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class MapDescriptorTest extends SessionFactoryBasedFunctionalTest {
	private EntityDescriptor entityDescriptor;

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( EntityOfMaps.class );
	}

	@BeforeEach
	public void findEntityDescriptor() {
		entityDescriptor = sessionFactory().getTypeConfiguration().findEntityDescriptor( EntityOfMaps.class );
	}

	@Test
	public void testBasicToBasic() {
		final PluralPersistentAttribute basicToBasic = (PluralPersistentAttribute) entityDescriptor.findPersistentAttribute( "basicToBasicMap" );

		assertThat(
				basicToBasic.getPersistentCollectionDescriptor().getIndexDescriptor().getClassification(),
				is( CollectionIndex.IndexClassification.BASIC )
		);
		assertEquals(
				String.class,
				basicToBasic.getPersistentCollectionDescriptor().getIndexDescriptor().getJavaTypeDescriptor().getJavaType()
		);

		assertThat(
				basicToBasic.getPersistentCollectionDescriptor().getElementDescriptor().getClassification(),
				is( CollectionElement.ElementClassification.BASIC )
		);
		assertEquals(
				String.class,
				basicToBasic.getPersistentCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getJavaType()
		);
	}

	@Test
	public void testBasicToManyToMany() {
		final PluralPersistentAttribute basicToBasic = (PluralPersistentAttribute) entityDescriptor.findPersistentAttribute( "basicToManyToMany" );

		assertThat(
				basicToBasic.getPersistentCollectionDescriptor().getIndexDescriptor().getClassification(),
				is( CollectionIndex.IndexClassification.BASIC )
		);
		assertEquals(
				String.class,
				basicToBasic.getPersistentCollectionDescriptor().getIndexDescriptor().getJavaTypeDescriptor().getJavaType()
		);

		assertThat(
				basicToBasic.getPersistentCollectionDescriptor().getElementDescriptor().getClassification(),
				is( CollectionElement.ElementClassification.MANY_TO_MANY )
		);
		assertEquals(
				EntityOfMaps.class,
				basicToBasic.getPersistentCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getJavaType()
		);
	}
}
