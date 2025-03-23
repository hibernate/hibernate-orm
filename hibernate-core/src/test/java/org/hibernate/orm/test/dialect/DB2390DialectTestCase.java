/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.DB2zDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11747")
@RequiresDialect(DB2zDialect.class)
public class DB2390DialectTestCase extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	@Test
	public void testLegacyLimitHandlerWithNoOffset() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<SimpleEntity> results = entityManager.createQuery( "FROM SimpleEntity", SimpleEntity.class )
					.setMaxResults( 2 )
					.getResultList();
			assertEquals( Arrays.asList( 0, 1 ), results.stream().map( SimpleEntity::getId ).collect( Collectors.toList() ) );
		} );
	}

	@Test
	public void testLegacyLimitHandlerWithOffset() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<SimpleEntity> results = entityManager.createQuery( "FROM SimpleEntity", SimpleEntity.class )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.getResultList();
			assertEquals( Arrays.asList( 2, 3 ), results.stream().map( SimpleEntity::getId ).collect( Collectors.toList() ) );
		} );
	}

	@Before
	public void populateSchema() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < 10; ++i ) {
				final SimpleEntity simpleEntity = new SimpleEntity( i, "Entity" + i );
				entityManager.persist( simpleEntity );
			}
		} );
	}

	@After
	public void cleanSchema() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "DELETE FROM SimpleEntity" ).executeUpdate();
		} );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;

		public SimpleEntity() {

		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
