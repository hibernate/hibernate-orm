/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.mappedsuperclass;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hamcrest.MatcherAssert;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-11549")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/version/mappedsuperclass/TestEntity.hbm.xml",
		annotatedClasses = AbstractEntity.class
)
@SessionFactory
public class HbmMappingMappedSuperclassWithVersionTest {

	@Test
	public void testMetamodelContainsHbmVersion(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			final TestEntity entity = new TestEntity();
			entity.setName( "Chris" );
			entityManager.persist( entity );
		} );

		factoryScope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<TestEntity> query = builder.createQuery( TestEntity.class );
			final Root<TestEntity> root = query.from( TestEntity.class );

			MatcherAssert.assertThat( root.get( "version" ), is( notNullValue() ) );
		} );
	}

}
