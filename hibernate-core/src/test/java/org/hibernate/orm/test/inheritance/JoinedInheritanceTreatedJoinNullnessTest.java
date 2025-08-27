/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinedInheritanceTreatedJoinNullnessTest.AbstractCompany.class,
		JoinedInheritanceTreatedJoinNullnessTest.AbstractDcCompany.class,
		JoinedInheritanceTreatedJoinNullnessTest.DcCompany.class,
		JoinedInheritanceTreatedJoinNullnessTest.DcCompanySeed.class,
		JoinedInheritanceTreatedJoinNullnessTest.RcCompany.class,
		JoinedInheritanceTreatedJoinNullnessTest.RcCompanyUser.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17711" )
public class JoinedInheritanceTreatedJoinNullnessTest {
	@Test
	public void testRootIsNotNull(SessionFactoryScope scope) {
		executeQuery( scope, 2, (cb, cq, root) -> cq.where( cb.isNotNull( cb.treat( root, DcCompanySeed.class ) ) ) );
	}

	@Test
	public void testJoinIsNotNull(SessionFactoryScope scope) {
		executeQuery( scope, 1, (cb, cq, root) -> cq.where( cb.isNotNull(
				cb.treat( root, DcCompanySeed.class ).join( "invitedBy", JoinType.LEFT )
		) ) );
	}

	@Test
	public void testNestedJoinIsNotNull(SessionFactoryScope scope) {
		executeQuery( scope, 1, (cb, cq, root) -> cq.where( cb.isNotNull(
				cb.treat( root, DcCompanySeed.class )
						.join( "invitedBy", JoinType.LEFT )
						.join( "rcCompany", JoinType.LEFT )
		) ) );
	}

	@Test
	public void testEitherJoinNotNull(SessionFactoryScope scope) {
		executeQuery( scope, 2, (cb, cq, root) -> {
			final Predicate seedPredicate = cb.and(
					cb.equal( root.get( "displayName" ), "test" ),
					cb.isNotNull( cb.treat( root, DcCompanySeed.class ).join( "invitedBy", JoinType.LEFT ).get( "rcCompany" ) )
			);
			final Predicate dcPredicate = cb.and(
					cb.equal( root.get( "displayName" ), "test" ),
					cb.isNotNull( cb.treat( root, DcCompany.class ).get( "rcCompany" ) )
			);
			cq.where( cb.or( seedPredicate, dcPredicate ) );
		} );
	}

	private void executeQuery(SessionFactoryScope scope, int expectedResults, CriteriaConsumer consumer) {
		// Test id selection
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Long> cq = cb.createQuery( Long.class );
			final Root<AbstractDcCompany> root = cq.from( AbstractDcCompany.class );
			consumer.apply( cb, cq, root );
			final List<Long> resultList = session.createQuery( cq.select( root.get( "id" ) ) ).getResultList();
			assertThat( resultList ).hasSize( expectedResults );
		} );
		// Test root selection
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<AbstractDcCompany> cq = cb.createQuery( AbstractDcCompany.class );
			final Root<AbstractDcCompany> root = cq.from( AbstractDcCompany.class );
			consumer.apply( cb, cq, root );
			final List<AbstractDcCompany> resultList = session.createQuery( cq.select( root ) ).getResultList();
			assertThat( resultList ).hasSize( expectedResults );
		} );
	}

	public interface CriteriaConsumer {
		void apply(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<AbstractDcCompany> root);
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final RcCompany rc = new RcCompany( "rc" );
			session.persist( rc );
			final RcCompanyUser rcu = new RcCompanyUser( rc );
			session.persist( rcu );
			session.persist( new DcCompanySeed( "test", rcu ) );
			session.persist( new DcCompanySeed( "test", null ) );
			session.persist( new DcCompany( "test", rc ) );
			session.persist( new DcCompany( "test", null ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from DcCompanySeed" ).executeUpdate();
			session.createMutationQuery( "delete from RcCompanyUser" ).executeUpdate();
			session.createMutationQuery( "delete from DcCompany" ).executeUpdate();
			session.createMutationQuery( "delete from AbstractCompany" ).executeUpdate();
		} );
	}

	@Entity( name = "AbstractCompany" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class AbstractCompany {
		@Id
		@GeneratedValue
		private Long id;

		@Column
		private String displayName;

		public AbstractCompany() {
		}

		public AbstractCompany(String displayName) {
			this.displayName = displayName;
		}
	}

	@Entity( name = "AbstractDcCompany" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public abstract static class AbstractDcCompany extends AbstractCompany {
		public AbstractDcCompany() {
		}

		public AbstractDcCompany(String displayName) {
			super( displayName );
		}
	}

	@Entity( name = "DcCompany" )
	public static class DcCompany extends AbstractDcCompany {
		@ManyToOne( fetch = FetchType.LAZY )
		private RcCompany rcCompany;

		public DcCompany() {
		}

		public DcCompany(String displayName, RcCompany rcCompany) {
			super( displayName );
			this.rcCompany = rcCompany;
		}
	}

	@Entity( name = "DcCompanySeed" )
	public static class DcCompanySeed extends AbstractDcCompany {
		@ManyToOne
		private RcCompanyUser invitedBy;

		public DcCompanySeed() {
		}

		public DcCompanySeed(String displayName, RcCompanyUser invitedBy) {
			super( displayName );
			this.invitedBy = invitedBy;
		}
	}

	@Entity( name = "RcCompany" )
	public static class RcCompany extends AbstractCompany {
		public RcCompany() {
		}

		public RcCompany(String displayName) {
			super( displayName );
		}
	}

	@Entity( name = "RcCompanyUser" )
	public static class RcCompanyUser {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		private RcCompany rcCompany;

		public RcCompanyUser() {
		}

		public RcCompanyUser(RcCompany rcCompany) {
			this.rcCompany = rcCompany;
		}
	}
}
