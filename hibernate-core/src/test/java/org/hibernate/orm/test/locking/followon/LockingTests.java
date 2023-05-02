/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.locking.followon;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(settings = @Setting(name = AvailableSettings.JPA_COMPLIANCE, value = "true"))
@DomainModel(annotatedClasses = {SomeEntity.class, DependentEntity.class})
@SessionFactory
public class LockingTests {
	@Test
	public void testQueryLockingBaseline(SessionFactoryScope scope) {
		testQueryLocking( scope, null );
	}

	@Test
	public void testQueryLocking(SessionFactoryScope scope) {
		testQueryLocking( scope, true );
	}

	private void testQueryLocking(SessionFactoryScope scope, Boolean useFollowOnLocking) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			//noinspection deprecation
			final Query query = session.createQuery( "select e from SomeEntity e where e.id < 10" )
					.setHint( SpecHints.HINT_SPEC_LOCK_SCOPE, PessimisticLockScope.EXTENDED )
					.setHint( HibernateHints.HINT_FOLLOW_ON_LOCKING, useFollowOnLocking )
					.setLockMode(LockModeType.PESSIMISTIC_READ);

			query.setLockMode(LockModeType.PESSIMISTIC_READ);
			assertThat( query.getLockMode() ).isEqualTo(LockModeType.PESSIMISTIC_READ);

			final List<SomeEntity> resultList = query.getResultList();
			for ( SomeEntity someEntity : resultList ) {
				final LockModeType appliedLockMode = session.getLockMode( someEntity );
				assertThat( appliedLockMode ).isIn( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE );

				final LockModeType dependentLockMode = session.getLockMode( someEntity.getDependent() );
				assertThat( dependentLockMode ).isIn( LockModeType.READ, LockModeType.OPTIMISTIC );
			}

			if ( useFollowOnLocking ) {
				// * the Query
				// * load DependentEntity#1
				// * lock SomeEntity#1
				// * lock SomeEntity#2
				assertThat( statementInspector.getSqlQueries() ).hasSize( 4 );
			}

			// try to update the SomeEntity while it is locked in the main session
			try {
				scope.inTransaction( (session2) -> {
					final SomeEntity first = session2.find( SomeEntity.class, 1 );
					first.setName( "xyz" );
				} );
				fail( "Locked entity update was allowed" );
			}
			catch (PessimisticLockException | LockTimeoutException expected) {
			}
		} );
	}

	@Test
	public void testJoinedQueryLockingBaseline(SessionFactoryScope scope) {
		testJoinedQueryLocking( scope, null );
	}

	@Test
	public void testJoinQueryLocking(SessionFactoryScope scope) {
		testJoinedQueryLocking( scope, true );
	}

	private void testJoinedQueryLocking(SessionFactoryScope scope, Boolean useFollowOnLocking) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Query query = session.createQuery("select e from SomeEntity e join fetch dependent where e.id < 10" );
			query.setHint( HibernateHints.HINT_FOLLOW_ON_LOCKING, useFollowOnLocking );
			assertThat( query.getLockMode() ).isIn( null, LockModeType.NONE );

			query.setLockMode(LockModeType.PESSIMISTIC_READ);
			assertThat( query.getLockMode() ).isEqualTo(LockModeType.PESSIMISTIC_READ);

			final List<SomeEntity> resultList = query.getResultList();
			for ( SomeEntity someEntity : resultList ) {
				final LockModeType appliedLockMode = session.getLockMode( someEntity );
				assertThat( appliedLockMode ).isIn( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE );

				final LockModeType dependentLockMode = session.getLockMode( someEntity.getDependent() );
				assertThat( dependentLockMode ).isIn( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE );
			}

			if ( useFollowOnLocking ) {
				// * the Query
				// * SomeEntity#1 lock
				// * SomeEntity#2 lock
				// * DependentEntity#1 lock
				assertThat( statementInspector.getSqlQueries() ).hasSize( 4 );
			}

			// try to update the SomeEntity while it is locked in the main session
			try {
				scope.inTransaction( (session2) -> {
					final SomeEntity first = session2.find( SomeEntity.class, 1 );
					first.setName( "xyz" );
				} );
				fail( "Locked entity update was allowed" );
			}
			catch (PessimisticLockException | LockTimeoutException expected) {
			}
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final DependentEntity dependent = new DependentEntity( 1, "dependent" );
			session.persist( dependent );
			session.persist( new SomeEntity( 1, "abc", dependent ) );
			session.persist( new SomeEntity( 2, "def", dependent ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete SomeEntity" ).executeUpdate();
			session.createMutationQuery( "delete DependentEntity" ).executeUpdate();
		} );
	}
}
