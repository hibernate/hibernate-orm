/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.mappedsuperclass;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11549")
public class HbmMappingMappedSuperclassWithVersionTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {"org/hibernate/orm/test/version/mappedsuperclass/TestEntity.hbm.xml"};
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {AbstractEntity.class};
	}

	@Test
	public void testMetamodelContainsHbmVersion() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final TestEntity entity = new TestEntity();
			entity.setName( "Chris" );
			entityManager.persist( entity );
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<TestEntity> query = builder.createQuery( TestEntity.class );
			final Root<TestEntity> root = query.from( TestEntity.class );

			assertThat( root.get( "version" ), is( notNullValue() ) );
		} );
	}

}
