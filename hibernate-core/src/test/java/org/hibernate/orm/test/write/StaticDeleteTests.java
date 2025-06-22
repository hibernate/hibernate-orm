/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = StaticDeleteTests.SimpleEntity.class )
@SessionFactory
public class StaticDeleteTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new SimpleEntity( 1, "stuff" ) );
		} );

		scope.inTransaction( (session) -> {
			final SimpleEntity entity = session.get( SimpleEntity.class, 1 );
			assertThat( entity ).isNotNull();

			session.remove( entity );
		} );

		scope.inTransaction( (session) -> {
			final SimpleEntity entity = session.find( SimpleEntity.class, 1 );
			assertThat( entity ).isNull();
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "SimpleEntity" )
	public static class SimpleEntity {
		@Id
		private Integer id;
		@Basic
		private String name;

		private SimpleEntity() {
			// for use by Hibernate
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
