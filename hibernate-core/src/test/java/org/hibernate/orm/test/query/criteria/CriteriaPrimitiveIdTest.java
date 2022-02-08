/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

@TestForIssue(jiraKey = "HHH-15073")
public class CriteriaPrimitiveIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MyEntity.class };
	}

	@Test
	public void test() {
		inTransaction( session -> {
			session.persist( new MyEntity( 1L ) );
			session.persist( new MyEntity( 2L ) );
			session.persist( new MyEntity( 3L ) );
		} );
		inTransaction( session -> {
			EntityDomainType<MyEntity> type = sessionFactory().getJpaMetamodel().entity( MyEntity.class );
			@SuppressWarnings("unchecked")
			SingularPersistentAttribute<MyEntity, Long> idAttribute =
					(SingularPersistentAttribute<MyEntity, Long>) type.findIdAttribute();
			Query<Long> query = createQueryForIdentifierListing( session, type, idAttribute );
			assertThat( query.list() ).containsExactlyInAnyOrder( 1L, 2L, 3L );
		} );
	}

	private <E, I> Query<I> createQueryForIdentifierListing(Session session,
			EntityDomainType<E> type, SingularAttribute<? super E, I> idAttribute) {
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

		public MyEntity() {
		}

		public MyEntity(long id) {
			this.id = id;
		}
	}
}
