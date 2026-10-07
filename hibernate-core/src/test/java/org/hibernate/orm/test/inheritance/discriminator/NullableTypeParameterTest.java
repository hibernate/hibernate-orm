package org.hibernate.orm.test.inheritance.discriminator;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.HibernateException;
import org.hibernate.query.QueryParameter;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Class-valued TYPE parameters, including the optional scalar and collection filters from HHH-12568.
///
/// @author Steve Ebersole
@JiraKey("HHH-12568")
@DomainModel(annotatedClasses = {
		NullableTypeParameterTest.Item.class,
		NullableTypeParameterTest.SingleRoot.class,
		NullableTypeParameterTest.SingleA.class,
		NullableTypeParameterTest.SingleB.class,
		NullableTypeParameterTest.JoinedRoot.class,
		NullableTypeParameterTest.JoinedA.class,
		NullableTypeParameterTest.JoinedB.class,
		NullableTypeParameterTest.UnionRoot.class,
		NullableTypeParameterTest.UnionA.class,
		NullableTypeParameterTest.UnionB.class
})
@SessionFactory
public class NullableTypeParameterTest {
	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Item[] items = {
					new SingleRoot(), new SingleA(), new SingleB(),
					new JoinedRoot(), new JoinedA(), new JoinedB(),
					new UnionRoot(), new UnionA(), new UnionB()
			};
			for ( int i = 0; i < items.length; i++ ) {
				items[i].id = i % 3 + 1;
				items[i].name = switch ( i % 3 ) {
					case 0 -> "root";
					case 1 -> "a";
					default -> "b";
				};
				session.persist( items[i] );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@EnumSource(Strategy.class)
	void typeEquality(Strategy strategy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var query = session.createQuery( "select e.name from " + strategy.root.getSimpleName()
					+ " e where type(e) = :type", String.class );
			assertThat( query.setParameter( "type", null ).getResultList() ).isEmpty();
			assertThat( query.setParameter( "type", strategy.a ).getResultList() ).containsExactly( "a" );
			assertThat( query.setParameter( "type", strategy.b ).getResultList() ).containsExactly( "b" );
			assertThat( query.setParameter( "type", strategy.root ).getResultList() ).containsExactly( "root" );
			assertThat( query.setParameter( "type", null ).getResultList() ).isEmpty();
		} );
	}

	@ParameterizedTest
	@EnumSource(Strategy.class)
	void optionalTypeEquality(Strategy strategy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var query = session.createQuery( "select e.name from " + strategy.root.getSimpleName()
					+ " e where type(e) = :type or :type is null", String.class );
			assertThat( query.setParameter( "type", null ).getResultList() )
					.containsExactlyInAnyOrder( "root", "a", "b" );
			assertThat( query.setParameter( "type", strategy.a ).getResultList() ).containsExactly( "a" );
			assertThat( query.setParameter( "type", strategy.root ).getResultList() ).containsExactly( "root" );
			assertThat( query.setParameter( "type", null ).getResultList() )
					.containsExactlyInAnyOrder( "root", "a", "b" );
		} );
	}

	@ParameterizedTest
	@EnumSource(Strategy.class)
	void typeInCollection(Strategy strategy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var query = session.createQuery( "select e.name from " + strategy.root.getSimpleName()
					+ " e where type(e) in (:types)", String.class );
			assertThat( query.setParameter( "types", null ).getResultList() ).isEmpty();
			assertThat( query.setParameterList( "types", List.of( strategy.a ) ).getResultList() ).containsExactly( "a" );
			assertThat( query.setParameterList( "types", List.of( strategy.a, strategy.b ) ).getResultList() )
					.containsExactlyInAnyOrder( "a", "b" );
			assertThat( query.setParameterList( "types", List.of( strategy.root ) ).getResultList() ).containsExactly( "root" );
		} );
	}

	@ParameterizedTest
	@EnumSource(Strategy.class)
	@FailureExpected(jiraKey = "HHH-12568", reason = "Rebinding a collection parameter to null retains its previous multi-valued binding")
	void collectionParameterReboundToNull(Strategy strategy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var query = session.createQuery( "select e.name from " + strategy.root.getSimpleName()
					+ " e where type(e) in (:types)", String.class );
			assertThat( query.setParameterList( "types", List.of( strategy.a ) ).getResultList() ).containsExactly( "a" );
			assertThat( query.setParameter( "types", null ).getResultList() ).isEmpty();
		} );
	}

	@ParameterizedTest
	@EnumSource(Strategy.class)
	void optionalTypeInCollectionRejectsList(Strategy strategy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var query = session.createQuery( "select e.name from " + strategy.root.getSimpleName()
					+ " e where :types is null or (:types is not null and type(e) in (:types))", String.class );
			assertThat( query.setParameter( "types", null ).getResultList() )
					.containsExactlyInAnyOrder( "root", "a", "b" );
			// The IS NULL occurrences make this a scalar parameter, despite its use in IN.
			assertThat( ( (QueryParameter<?>) query.getParameter( "types" ) ).allowsMultiValuedBinding() ).isFalse();
			assertThatThrownBy( () -> query.setParameter( "types", List.of( strategy.a ) ).getResultList() )
					.isInstanceOf( HibernateException.class )
					.hasMessageContaining( "java.lang.Class" );
		} );
	}

	public enum Strategy {
		SINGLE_TABLE( SingleRoot.class, SingleA.class, SingleB.class ),
		JOINED( JoinedRoot.class, JoinedA.class, JoinedB.class ),
		TABLE_PER_CLASS( UnionRoot.class, UnionA.class, UnionB.class );

		final Class<? extends Item> root;
		final Class<? extends Item> a;
		final Class<? extends Item> b;

		Strategy(Class<? extends Item> root, Class<? extends Item> a, Class<? extends Item> b) {
			this.root = root;
			this.a = a;
			this.b = b;
		}
	}

	@MappedSuperclass
	public abstract static class Item {
		@Id
		int id;
		String name;
	}

	@Entity(name = "SingleRoot")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class SingleRoot extends Item {
	}

	@Entity(name = "SingleA")
	public static class SingleA extends SingleRoot {
	}

	@Entity(name = "SingleB")
	public static class SingleB extends SingleRoot {
	}

	@Entity(name = "JoinedRoot")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class JoinedRoot extends Item {
	}

	@Entity(name = "JoinedA")
	public static class JoinedA extends JoinedRoot {
	}

	@Entity(name = "JoinedB")
	public static class JoinedB extends JoinedRoot {
	}

	@Entity(name = "UnionRoot")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class UnionRoot extends Item {
	}

	@Entity(name = "UnionA")
	public static class UnionA extends UnionRoot {
	}

	@Entity(name = "UnionB")
	public static class UnionB extends UnionRoot {
	}
}
