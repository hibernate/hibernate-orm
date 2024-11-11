/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.metamodel;


import org.hibernate.metamodel.model.domain.EmbeddableDomainType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.metamodel.Type.PersistenceType.MAPPED_SUPERCLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Jpa( annotatedClasses = {
		ProductEntity.class,
		LocalizedValue.class,
		Company.class,
		Address.class,
		Person.class,
		Measurement.class,
		Height.class,
		WeightClass.class,
		Weight.class,
} )
public class EmbeddableMetaModelTest {
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-11111" )
	public void testEmbeddableCanBeResolvedWhenUsedAsInterface(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertNotNull( entityManager.getMetamodel().embeddable( LocalizedValue.class ) );
			assertEquals( LocalizedValue.class, ProductEntity_.description.getElementType().getJavaType() );
			assertNotNull( LocalizedValue_.value );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-12124" )
	public void testEmbeddableEquality(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertTrue( entityManager.getMetamodel().getEmbeddables().contains( Company_.address.getType() ) );
			assertTrue( entityManager.getMetamodel().getEmbeddables().contains( Person_.height.getType() ) );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18103" )
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final EmbeddableDomainType<Height> embeddable = (EmbeddableDomainType<Height>) entityManager.getMetamodel()
					.embeddable( Height.class );
			assertNotNull( embeddable.getSuperType() );
			assertEquals( MAPPED_SUPERCLASS, embeddable.getSuperType().getPersistenceType() );
			assertEquals( Measurement.class, embeddable.getSuperType().getJavaType() );
			assertNotNull( Height_.height );
			assertNotNull( Measurement_.unit );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18819" )
	public void testIdClass(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final EmbeddableDomainType<Weight> embeddable = (EmbeddableDomainType<Weight>) entityManager.getMetamodel()
					.embeddable( Weight.class );
			assertNotNull( embeddable.getSuperType() );
			assertEquals( MAPPED_SUPERCLASS, embeddable.getSuperType().getPersistenceType() );
			assertEquals( Measurement.class, embeddable.getSuperType().getJavaType() );
			assertNotNull( Weight_.weight );
			assertNotNull( Measurement_.unit );
		} );
	}
}
