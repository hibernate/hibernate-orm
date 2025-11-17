/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ql;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		TreatKeywordTest.JoinedEntity.class, TreatKeywordTest.JoinedEntitySubclass.class, TreatKeywordTest.JoinedEntitySubSubclass.class,
		TreatKeywordTest.JoinedEntitySubclass2.class, TreatKeywordTest.JoinedEntitySubSubclass2.class,
		TreatKeywordTest.DiscriminatorEntity.class, TreatKeywordTest.DiscriminatorEntitySubclass.class, TreatKeywordTest.DiscriminatorEntitySubSubclass.class,
		TreatKeywordTest.Animal.class, TreatKeywordTest.Dog.class, TreatKeywordTest.Dachshund.class, TreatKeywordTest.Greyhound.class
})
@SessionFactory
public class TreatKeywordTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testBasicUsageInJoin(SessionFactoryScope scope) {
		// todo : assert invalid naming of non-subclasses in TREAT statement
		scope.inSession( s -> {
			s.createQuery( "from DiscriminatorEntity e join treat(e.other as DiscriminatorEntitySubclass) o", Object[].class ).list();
			s.createQuery( "from DiscriminatorEntity e join treat(e.other as DiscriminatorEntitySubSubclass) o", Object[].class ).list();
			s.createQuery( "from DiscriminatorEntitySubclass e join treat(e.other as DiscriminatorEntitySubSubclass) o", Object[].class ).list();

			s.createQuery( "from JoinedEntity e join treat(e.other as JoinedEntitySubclass) o", Object[].class ).list();
			s.createQuery( "from JoinedEntity e join treat(e.other as JoinedEntitySubSubclass) o", Object[].class ).list();
			s.createQuery( "from JoinedEntitySubclass e join treat(e.other as JoinedEntitySubSubclass) o", Object[].class ).list();
		} );
	}

	@Test
	@JiraKey( value = "HHH-8637" )
	public void testFilteringDiscriminatorSubclasses(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			DiscriminatorEntity root = new DiscriminatorEntity( 1, "root" );
			s.persist( root );
			DiscriminatorEntitySubclass child = new DiscriminatorEntitySubclass( 2, "child", root );
			s.persist( child );
		} );

		scope.inSession( s -> {
			// in select clause
			List<Object[]> result = s.createQuery( "select e from DiscriminatorEntity e", Object[].class ).list();
			assertEquals( 2, result.size() );
			result = s.createQuery( "select treat (e as DiscriminatorEntitySubclass) from DiscriminatorEntity e", Object[].class ).list();
			assertEquals( 1, result.size() );
			result = s.createQuery( "select treat (e as DiscriminatorEntitySubSubclass) from DiscriminatorEntity e", Object[].class ).list();
			assertEquals( 0, result.size() );

			// in join
			List<DiscriminatorEntity> result2 = s.createQuery( "from DiscriminatorEntity e inner join e.other", DiscriminatorEntity.class ).list();
			assertEquals( 1, result2.size() );
			result2 = s.createQuery( "from DiscriminatorEntity e inner join treat (e.other as DiscriminatorEntitySubclass)", DiscriminatorEntity.class ).list();
			assertEquals( 0, result2.size() );
			result2 = s.createQuery( "from DiscriminatorEntity e inner join treat (e.other as DiscriminatorEntitySubSubclass)", DiscriminatorEntity.class ).list();
			assertEquals( 0, result2.size() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-8637" )
	public void testFilteringJoinedSubclasses(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			JoinedEntity root = new JoinedEntity( 1, "root" );
			s.persist( root );
			JoinedEntitySubclass child = new JoinedEntitySubclass( 2, "child", root );
			s.persist( child );
		} );

		scope.inSession( s -> {
			// in the select clause which causes an implicit inclusion of subclass joins, the test here makes sure that
			// the TREAT-AS effects the join-type used.
			List result = s.createQuery( "select e from JoinedEntity e" ).list();
			assertEquals( 2, result.size() );
			result = s.createQuery( "select treat (e as JoinedEntitySubclass) from JoinedEntity e" ).list();
			assertEquals( 1, result.size() );
			result = s.createQuery( "select e from JoinedEntity e where treat (e as JoinedEntitySubclass) is not null" ).list();
			assertEquals( 1, result.size() );
			result = s.createQuery( "select treat (e as JoinedEntitySubSubclass) from JoinedEntity e" ).list();
			assertEquals( 0, result.size() );

			// in join
			result = s.createQuery( "from JoinedEntity e inner join e.other", JoinedEntity.class ).list();
			assertEquals( 1, result.size() );
			result = s.createQuery( "from JoinedEntity e inner join treat (e.other as JoinedEntitySubclass)", JoinedEntity.class ).list();
			assertEquals( 0, result.size() );
			result = s.createQuery( "from JoinedEntity e inner join treat (e.other as JoinedEntitySubSubclass)", JoinedEntity.class ).list();
			assertEquals( 0, result.size() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-9862" )
	public void testRestrictionsOnJoinedSubclasses(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			JoinedEntity root = new JoinedEntity( 1, "root" );
			s.persist( root );
			JoinedEntitySubclass child1 = new JoinedEntitySubclass( 2, "child1", root );
			s.persist( child1 );
			JoinedEntitySubclass2 child2 = new JoinedEntitySubclass2( 3, "child2", root );
			s.persist( child2 );
		} );

		scope.inSession( s -> {
			List result = s.createQuery( "select e from JoinedEntity e where treat (e as JoinedEntitySubclass ).name = 'child1'" ).list();
			assertEquals( 1, result.size() );
			assertTrue( JoinedEntitySubclass.class.isInstance( result.get( 0 ) ) );

			result = s.createQuery( "select e from JoinedEntity e where treat (e as JoinedEntitySubclass2 ).name = 'child1'" ).list();
			assertEquals( 0, result.size() );

			result = s.createQuery( "select e from JoinedEntity e where treat (e as JoinedEntitySubclass2 ).name = 'child2'" ).list();
			assertEquals( 1, result.size() );
			assertTrue( JoinedEntitySubclass2.class.isInstance( result.get( 0 ) ) );

			result = s.createQuery( "select e from JoinedEntity e where treat (e as JoinedEntitySubclass ).name = 'child1' or treat (e as JoinedEntitySubclass2 ).name = 'child2'" ).list();
			assertEquals( 2, result.size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9411")
	public void testTreatWithRestrictionOnAbstractClass(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Greyhound greyhound = new Greyhound();
					Dachshund dachshund = new Dachshund();
					s.persist( greyhound );
					s.persist( dachshund );

					List results = s.createQuery( "select treat (a as Dog) from Animal a where a.fast = TRUE" ).list();

					assertEquals( List.of( greyhound ), results );
					s.remove( greyhound );
					s.remove( dachshund );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16657")
	public void testTypeFilterInSubquery(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					JoinedEntitySubclass2 child1 = new JoinedEntitySubclass2(3, "child1");
					JoinedEntitySubSubclass2 child2 = new JoinedEntitySubSubclass2(4, "child2");
					JoinedEntitySubclass root1 = new JoinedEntitySubclass(1, "root1", child1);
					JoinedEntitySubSubclass root2 = new JoinedEntitySubSubclass(2, "root2", child2);
					s.persist( child1 );
					s.persist( child2 );
					s.persist( root1 );
					s.persist( root2 );
				}
		);
		scope.inSession(
				s -> {
					List<String> results = s.createSelectionQuery(
							"select (select o.name from j.other o where type(j) = JoinedEntitySubSubclass) from JoinedEntitySubclass j order by j.id",
							String.class
					).list();

					assertEquals( 2, results.size() );
					assertNull( results.get( 0 ) );
					assertEquals( "child2", results.get( 1 ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16658")
	public void testPropagateEntityNameUsesFromDisjunction(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					s.createSelectionQuery(
							"select 1 from Animal a where (type(a) <> Dachshund or treat(a as Dachshund).fast) and (type(a) <> Greyhound or treat(a as Greyhound).fast)",
							Integer.class
					).list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16658")
	public void testPropagateEntityNameUsesFromDisjunction2(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					s.createSelectionQuery(
							"select 1 from JoinedEntity j where type(j) <> JoinedEntitySubclass or length(coalesce(treat(j as JoinedEntitySubclass).name,'')) > 1",
							Integer.class
					).list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16657")
	public void testTreatInSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					JoinedEntitySubclass root1 = new JoinedEntitySubclass(1, "root1");
					JoinedEntitySubSubclass root2 = new JoinedEntitySubSubclass(2, "root2");
					s.persist( root1 );
					s.persist( root2 );
				}
		);
		scope.inSession(
				s -> {
					List<String> results = s.createSelectionQuery(
							"select treat(j as JoinedEntitySubSubclass).name from JoinedEntitySubclass j order by j.id",
							String.class
					).list();

					assertEquals( 2, results.size() );
					assertNull( results.get( 0 ) );
					assertEquals( "root2", results.get( 1 ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16571") // Sort of related to that issue
	public void testJoinSubclassOneToMany(SessionFactoryScope scope) {
		// Originally, the FK for "others" used the primary key of the root table JoinedEntity
		// Since we didn't register an entity use, we wrongly pruned that table before.
		// This was fixed by letting the FK descriptor point to the primary key of JoinedEntitySubclass2,
		// i.e. the plural attribute declaring type, which has the nice benefit of saving us a join
		scope.inSession(
				s -> {
					s.createSelectionQuery(
							"select 1 from JoinedEntitySubclass2 s left join s.others o",
							Integer.class
					).list();
				}
		);
	}

	@Entity( name = "JoinedEntity" )
	@Table( name = "JoinedEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class JoinedEntity {
		@Id
		public Integer id;
		public String name;
		@ManyToOne( fetch = FetchType.LAZY )
		public JoinedEntity other;

		public JoinedEntity() {
		}

		public JoinedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public JoinedEntity(Integer id, String name, JoinedEntity other) {
			this.id = id;
			this.name = name;
			this.other = other;
		}
	}

	@Entity( name = "JoinedEntitySubclass" )
	@Table( name = "JoinedEntitySubclass" )
	public static class JoinedEntitySubclass extends JoinedEntity {
		public JoinedEntitySubclass() {
		}

		public JoinedEntitySubclass(Integer id, String name) {
			super( id, name );
		}

		public JoinedEntitySubclass(Integer id, String name, JoinedEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "JoinedEntitySubSubclass" )
	@Table( name = "JoinedEntitySubSubclass" )
	public static class JoinedEntitySubSubclass extends JoinedEntitySubclass {
		public JoinedEntitySubSubclass() {
		}

		public JoinedEntitySubSubclass(Integer id, String name) {
			super( id, name );
		}

		public JoinedEntitySubSubclass(Integer id, String name, JoinedEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "JoinedEntitySubclass2" )
	@Table( name = "JoinedEntitySubclass2" )
	public static class JoinedEntitySubclass2 extends JoinedEntity {
		@OneToMany(mappedBy = "other")
		Set<JoinedEntity> others;
		public JoinedEntitySubclass2() {
		}

		public JoinedEntitySubclass2(Integer id, String name) {
			super( id, name );
		}

		public JoinedEntitySubclass2(Integer id, String name, JoinedEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "JoinedEntitySubSubclass2" )
	@Table( name = "JoinedEntitySubSubclass2" )
	public static class JoinedEntitySubSubclass2 extends JoinedEntitySubclass2 {
		public JoinedEntitySubSubclass2() {
		}

		public JoinedEntitySubSubclass2(Integer id, String name) {
			super( id, name );
		}

		public JoinedEntitySubSubclass2(Integer id, String name, JoinedEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "DiscriminatorEntity" )
	@Table( name = "DiscriminatorEntity" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "e_type", discriminatorType = DiscriminatorType.STRING )
	@DiscriminatorValue( "B" )
	public static class DiscriminatorEntity {
		@Id
		public Integer id;
		public String name;
		@ManyToOne( fetch = FetchType.LAZY )
		public DiscriminatorEntity other;

		public DiscriminatorEntity() {
		}

		public DiscriminatorEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public DiscriminatorEntity(
				Integer id,
				String name,
				DiscriminatorEntity other) {
			this.id = id;
			this.name = name;
			this.other = other;
		}
	}

	@Entity( name = "DiscriminatorEntitySubclass" )
	@DiscriminatorValue( "S" )
	public static class DiscriminatorEntitySubclass extends DiscriminatorEntity {
		public DiscriminatorEntitySubclass() {
		}

		public DiscriminatorEntitySubclass(Integer id, String name) {
			super( id, name );
		}

		public DiscriminatorEntitySubclass(
				Integer id,
				String name,
				DiscriminatorEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "DiscriminatorEntitySubSubclass" )
	@DiscriminatorValue( "SS" )
	public static class DiscriminatorEntitySubSubclass extends DiscriminatorEntitySubclass {
		public DiscriminatorEntitySubSubclass() {
		}

		public DiscriminatorEntitySubSubclass(Integer id, String name) {
			super( id, name );
		}

		public DiscriminatorEntitySubSubclass(
				Integer id,
				String name,
				DiscriminatorEntity other) {
			super( id, name, other );
		}
	}

	@Entity(name = "Animal")
	public static abstract class Animal {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "Dog")
	public static abstract class Dog extends Animal {
		private boolean fast;

		protected Dog(boolean fast) {
			this.fast = fast;
		}

		public final boolean isFast() {
			return fast;
		}
	}

	@Entity(name = "Dachshund")
	public static class Dachshund extends Dog {
		public Dachshund() {
			super( false );
		}
	}

	@Entity(name = "Greyhound")
	public static class Greyhound extends Dog {
		public Greyhound() {
			super( true );
		}
	}
}
