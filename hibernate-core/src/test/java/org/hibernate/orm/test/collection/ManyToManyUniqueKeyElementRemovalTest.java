package org.hibernate.orm.test.collection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Removing an element from a set of entities whose join table references a unique key of the element,
 * rather than its primary key, deletes the row holding the unique key value of that element.
 */
@JiraKey("HHH-21248")
@DomainModel(
		annotatedClasses = {
				ManyToManyUniqueKeyElementRemovalTest.Owner.class,
				ManyToManyUniqueKeyElementRemovalTest.Target.class
		}
)
@SessionFactory
public class ManyToManyUniqueKeyElementRemovalTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testRemoveFromManyToManySet(SessionFactoryScope scope) {
		persistOwner( scope );
		scope.inTransaction( session -> {
			final Owner owner = session.find( Owner.class, 10 );
			assertThat( owner.set.remove( session.find( Target.class, 2 ) ) ).isTrue();
		} );
		assertThat( rows( scope, "select t_code from owner_set order by t_code", String.class ) )
				.containsExactly( "C1", "C3" );
	}

	@Test
	public void testRemoveFromOneToManySet(SessionFactoryScope scope) {
		persistOwner( scope );
		scope.inTransaction( session -> {
			final Owner owner = session.find( Owner.class, 10 );
			assertThat( owner.oneToMany.remove( session.find( Target.class, 2 ) ) ).isTrue();
		} );
		assertThat( rows( scope, "select t_code from owner_one_to_many order by t_code", String.class ) )
				.containsExactly( "C1", "C3" );
	}

	/**
	 * The unique key has the type of the identifier, and the number of each target is the identifier of
	 * another one, so that binding the identifier of the removed element deletes the row of another element.
	 */
	@Test
	public void testRemoveFromSetReferencingKeyOfIdentifierType(SessionFactoryScope scope) {
		persistOwner( scope );
		scope.inTransaction( session -> {
			final Owner owner = session.find( Owner.class, 10 );
			assertThat( owner.numbered.remove( session.find( Target.class, 1 ) ) ).isTrue();
		} );
		assertThat( rows( scope, "select t_number from owner_numbered order by t_number", Integer.class ) )
				.containsExactly( 1, 2 );
	}

	private static void persistOwner(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Owner owner = new Owner();
			for ( int i = 1; i <= 3; i++ ) {
				final Target target = new Target( i, "C" + i, 4 - i );
				session.persist( target );
				owner.set.add( target );
				owner.oneToMany.add( target );
				owner.numbered.add( target );
			}
			session.persist( owner );
		} );
	}

	/**
	 * The Java type of an integer column read by a native query depends on the database,
	 * unless the query is given a result type.
	 */
	private static <T> List<T> rows(SessionFactoryScope scope, String sql, Class<T> type) {
		return scope.fromTransaction( session -> session.createNativeQuery( sql, type ).getResultList() );
	}

	@Entity(name = "Owner")
	static class Owner {
		@Id
		int id = 10;

		@ManyToMany
		@JoinTable(name = "owner_set",
				inverseJoinColumns = @JoinColumn(name = "t_code", referencedColumnName = "code"))
		Set<Target> set = new HashSet<>();

		@OneToMany
		@JoinTable(name = "owner_one_to_many",
				inverseJoinColumns = @JoinColumn(name = "t_code", referencedColumnName = "code"))
		Set<Target> oneToMany = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "owner_numbered",
				inverseJoinColumns = @JoinColumn(name = "t_number", referencedColumnName = "target_number"))
		Set<Target> numbered = new HashSet<>();
	}

	@Entity(name = "Target")
	static class Target {
		@Id
		int id;

		@Column(unique = true)
		String code;

		@Column(name = "target_number", unique = true)
		int number;

		Target() {
		}

		Target(int id, String code, int number) {
			this.id = id;
			this.code = code;
			this.number = number;
		}
	}
}
