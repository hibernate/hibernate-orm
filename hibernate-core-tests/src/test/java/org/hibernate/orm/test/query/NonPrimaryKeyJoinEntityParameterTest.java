package org.hibernate.orm.test.query;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Entity parameters for explicit joins through a unique key distinct from the target identifier.
/// The association identifier mapping follows the reproducer attached to HHH-11977.
///
/// @author Steve Ebersole
@JiraKey("HHH-11977")
@DomainModel(annotatedClasses = {
		NonPrimaryKeyJoinEntityParameterTest.A.class,
		NonPrimaryKeyJoinEntityParameterTest.B.class
})
@SessionFactory(useCollectingStatementObserver = true)
public class NonPrimaryKeyJoinEntityParameterTest {
	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var target = new A( 1, 9 );
			var other = new A( 9, 1 );
			session.persist( target );
			session.persist( other );
			session.persist( new B( target ) );
			session.persist( new B( other ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void explicitJoinWithEntityParameter(boolean detached, boolean parameterFirst, SessionFactoryScope scope) {
		A detachedTarget = detached
				? scope.fromTransaction( session -> session.find( A.class, 1 ) )
				: null;
		scope.inTransaction( session -> {
			A target = detached ? detachedTarget : session.find( A.class, 1 );
			var statements = scope.getCollectingStatementObserver();
			statements.clear();
			var results = session.createQuery( "select b from B b join b.a a where "
					+ ( parameterFirst ? ":a = a" : "a = :a" ), B.class )
					.setParameter( "a", target )
					.getResultList();
			statements.assertQuery( 0 ).containsToken( "join" );
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).a.getPrimaryKey() ).isEqualTo( 1 );
			assertThat( results.get( 0 ).a.getUniqueKey() ).isEqualTo( 9 );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void implicitJoinWithEntityParameter(boolean detached, boolean parameterFirst, SessionFactoryScope scope) {
		A detachedTarget = detached
				? scope.fromTransaction( session -> session.find( A.class, 1 ) )
				: null;
		scope.inTransaction( session -> {
			A target = detached ? detachedTarget : session.find( A.class, 1 );
			var statements = scope.getCollectingStatementObserver();
			statements.clear();
			var results = session.createQuery( "select b from B b where "
					+ ( parameterFirst ? ":a = b.a" : "b.a = :a" ), B.class )
					.setParameter( "a", target )
					.getResultList();
			statements.assertQuery( 0 ).doesNotContain( "join" );
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).a.getPrimaryKey() ).isEqualTo( 1 );
			assertThat( results.get( 0 ).a.getUniqueKey() ).isEqualTo( 9 );
		} );
	}

	@Entity(name = "A")
	@Table(name = "tbl_a")
	public static class A {
		@Id
		@Column(name = "id")
		int primaryKey;

		@Column(name = "unique_key", nullable = false, unique = true)
		int uniqueKey;

		public A() {
		}

		A(int primaryKey, int uniqueKey) {
			this.primaryKey = primaryKey;
			this.uniqueKey = uniqueKey;
		}

		public int getPrimaryKey() {
			return primaryKey;
		}

		public int getUniqueKey() {
			return uniqueKey;
		}
	}

	@Entity(name = "B")
	@Table(name = "tbl_b")
	public static class B implements Serializable {
		@Id
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "id", referencedColumnName = "unique_key")
		A a;

		public B() {
		}

		B(A a) {
			this.a = a;
		}
	}
}
