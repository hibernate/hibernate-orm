/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(
		annotatedClasses = {
				ByteArrayNaturalIdTest.TestEntity.class,
		}
)
@SessionFactory
@JiraKey("HHH-18409")
public class ByteArrayNaturalIdTest {

	private static final String NATURAL_ID_1 = "N1";

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createMutationQuery( "delete TestEntity" ).executeUpdate()
		);
	}

	@Test
	public void testSelectByNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1L, new byte[] { 1, 2 }, NATURAL_ID_1 );
					session.persist( entity );
					TestEntity testEntity = session.byNaturalId( TestEntity.class )
							.using( "naturalId2", NATURAL_ID_1 )
							.using( "naturalId1", new byte[] { 1, 2 } )
							.load();

					assertThat( testEntity ).as( "Loading the entity by its natural id failed" ).isNotNull();
					TestEntity testEntity2 = session.byNaturalId( TestEntity.class )
							.using( "naturalId2", NATURAL_ID_1 )
							.using( "naturalId1", new byte[] { 1, 3 } )
							.load();
					assertThat( testEntity2 ).as( "Loading the entity using wrong natural id failed" ).isNull();

				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		@NaturalId
		private byte[] naturalId1;

		@NaturalId
		private String naturalId2;

		public TestEntity() {
		}

		public TestEntity(Long id, byte[] naturalId1, String naturalId2) {
			this.id = id;
			this.naturalId1 = naturalId1;
			this.naturalId2 = naturalId2;
		}


	}
}
