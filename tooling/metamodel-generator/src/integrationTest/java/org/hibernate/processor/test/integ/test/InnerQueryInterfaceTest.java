/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.query.JakartaQuery;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {
		InnerQueryInterfaceTest.MyEntity.class,
		InnerQueryInterfaceTest.MyEntity.Queries.class
})
@SessionFactory
class InnerQueryInterfaceTest {

	@Entity(name = "InnerQueryEntity")
	@Table(name = "integ_inner_query_entity")
	public static class MyEntity {
		@Id
		@GeneratedValue
		public Long id;
		public String name;

		public MyEntity() {
		}

		public MyEntity(String name) {
			this.name = name;
		}

		public interface Queries {
			EntityManager entityManager();

			@HQL("from InnerQueryEntity where name = :name")
			List<MyEntity> findByHql(String name);

			@JakartaQuery("from InnerQueryEntity where name = :name")
			List<MyEntity> findByJakarta(String name);
		}
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testNestedQueriesWithHqlAndJakartaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new MyEntity( "Hibernate" ) );
			session.persist( new MyEntity( "Hibernate" ) );
			session.persist( new MyEntity( "Other" ) );
		} );
		scope.inTransaction( session -> {
			var queries = new InnerQueryInterfaceTest_.MyEntity_._Queries( session );
			List<MyEntity> hqlResults = queries.findByHql( "Hibernate" );
			assertEquals( 2, hqlResults.size() );
			List<MyEntity> jakartaResults = queries.findByJakarta( "Hibernate" );
			assertEquals( 2, jakartaResults.size() );
		} );
	}
}
