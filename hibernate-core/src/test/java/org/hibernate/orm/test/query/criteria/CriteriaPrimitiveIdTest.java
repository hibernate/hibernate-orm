/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-15073")
@DomainModel(
		annotatedClasses = {
				CriteriaPrimitiveIdTest.MyEntity.class
		}
)
@SessionFactory
public class CriteriaPrimitiveIdTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new MyEntity( 1L ) );
			session.persist( new MyEntity( 2L ) );
			session.persist( new MyEntity( 3L ) );
		} );

		scope.inTransaction( session -> {
			EntityType<MyEntity> type = scope.getSessionFactory().getJpaMetamodel().entity( MyEntity.class );
			SingularAttribute<? super MyEntity, Long> idAttribute = type.getId( long.class );
			Query<Long> query = createQueryForIdentifierListing( session, type, idAttribute );
			assertThat( query.list() ).containsExactlyInAnyOrder( 1L, 2L, 3L );
		} );
	}

	private <E, I> Query<I> createQueryForIdentifierListing(
			Session session,
			EntityType<E> type, SingularAttribute<? super E, I> idAttribute) {
		CriteriaBuilder criteriaBuilder = session.getSessionFactory().getCriteriaBuilder();
		CriteriaQuery<I> criteriaQuery = criteriaBuilder.createQuery( idAttribute.getJavaType() );
		Root<E> root = criteriaQuery.from( type );
		Path<I> idPath = root.get( idAttribute );
		criteriaQuery.select( idPath );
		return session.createQuery( criteriaQuery );
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		private long id;

		private String name;

		public MyEntity() {
		}

		public MyEntity(long id) {
			this.id = id;
		}
	}
}
