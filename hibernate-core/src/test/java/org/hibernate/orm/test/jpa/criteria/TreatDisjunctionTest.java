/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				TreatDisjunctionTest.PAccountDirectory.class,
				TreatDisjunctionTest.PLDAPDirectory.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class TreatDisjunctionTest {

	@Test
	@JiraKey( value = "HHH-15726")
	public void testQuery(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				entityManager -> {
					PAccountDirectory base = new PAccountDirectory();
					base.setActive( true );
					entityManager.persist( base );

					PLDAPDirectory sub = new PLDAPDirectory();
					sub.setActive( false );
					sub.setOpenldap( true );
					entityManager.persist( sub );
					entityManager.flush();
					entityManager.clear();

					sqlStatementInterceptor.clear();

					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<PAccountDirectory> query = cb.createQuery( PAccountDirectory.class );
					Root<PAccountDirectory> root = query.from( PAccountDirectory.class );

					From<?, PLDAPDirectory> ldap = cb.treat( root, PLDAPDirectory.class );
					Predicate exp = cb.or(
							cb.equal( root.get( "active" ), true ),
							cb.equal( ldap.get( "openldap" ), true )
					);

					List<PAccountDirectory> directories = entityManager.createQuery( query.select( root ).where( exp ) )
							.getResultList();
					assertThat( directories, hasSize( 2 ) );

					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals(
							"select " +
									"pd1_0.id," +
									"pd1_0.DTYPE," +
									"pd1_0.active," +
									"pd1_0.openldap " +
									"from PAccountDirectory pd1_0 " +
									"where pd1_0.active=? " +
									"or pd1_0.openldap=?",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@MappedSuperclass
	public static abstract class BaseEntity {
		@Id
		@GeneratedValue
		private Long id;

		public BaseEntity() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "PAccountDirectory")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class PAccountDirectory extends BaseEntity {
		@Column(nullable = false)
		private boolean active;

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}
	}

	@Entity(name = "PLDAPDirectory")
	public static class PLDAPDirectory extends PAccountDirectory {
		@Column(nullable = true)
		private boolean openldap;

		public boolean isOpenldap() {
			return openldap;
		}

		public void setOpenldap(boolean openldap) {
			this.openldap = openldap;
		}
	}
}
