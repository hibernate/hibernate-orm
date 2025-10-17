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
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11747")
@RequiresDialect(DB2zDialect.class)
@Jpa(annotatedClasses =  {DB2390DialectTestCase.SimpleEntity.class})
public class DB2390DialectTestCase {

	@BeforeAll
	public void populateSchema(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			for ( int i = 0; i < 10; ++i ) {
				final SimpleEntity simpleEntity = new SimpleEntity( i, "Entity" + i );
				entityManager.persist( simpleEntity );
			}
		} );
	}

	@AfterAll
	public void cleanSchema(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> scope.getEntityManagerFactory().getSchemaManager().truncate() );
	}

	@Test
	public void testLegacyLimitHandlerWithNoOffset(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<SimpleEntity> results = entityManager.createQuery( "FROM SimpleEntity", SimpleEntity.class )
					.setMaxResults( 2 )
					.getResultList();
			assertEquals( Arrays.asList( 0, 1 ), results.stream().map( SimpleEntity::getId ).collect( Collectors.toList() ) );
		} );
	}

	@Test
	public void testLegacyLimitHandlerWithOffset(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<SimpleEntity> results = entityManager.createQuery( "FROM SimpleEntity", SimpleEntity.class )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.getResultList();
			assertEquals( Arrays.asList( 2, 3 ), results.stream().map( SimpleEntity::getId ).collect( Collectors.toList() ) );
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
