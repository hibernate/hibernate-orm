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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class TreatKeywordTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				JoinedEntity.class, JoinedEntitySubclass.class, JoinedEntitySubSubclass.class,
				JoinedEntitySubclass2.class, JoinedEntitySubSubclass2.class,
				DiscriminatorEntity.class, DiscriminatorEntitySubclass.class, DiscriminatorEntitySubSubclass.class,
				Animal.class, Dog.class, Dachshund.class, Greyhound.class
		};
	}

	@Test
	public void testBasicUsageInJoin() {
		// todo : assert invalid naming of non-subclasses in TREAT statement
		Session s = openSession();

		s.createQuery( "from DiscriminatorEntity e join treat(e.other as DiscriminatorEntitySubclass) o", Object[].class ).list();
		s.createQuery( "from DiscriminatorEntity e join treat(e.other as DiscriminatorEntitySubSubclass) o", Object[].class ).list();
		s.createQuery( "from DiscriminatorEntitySubclass e join treat(e.other as DiscriminatorEntitySubSubclass) o", Object[].class ).list();

		s.createQuery( "from JoinedEntity e join treat(e.other as JoinedEntitySubclass) o", Object[].class ).list();
		s.createQuery( "from JoinedEntity e join treat(e.other as JoinedEntitySubSubclass) o", Object[].class ).list();
		s.createQuery( "from JoinedEntitySubclass e join treat(e.other as JoinedEntitySubSubclass) o", Object[].class ).list();

		s.close();
	}

	@Test
	@JiraKey( value = "HHH-8637" )
	public void testFilteringDiscriminatorSubclasses() {
		Session s = openSession();
		s.beginTransaction();
		DiscriminatorEntity root = new DiscriminatorEntity( 1, "root" );
		s.persist( root );
		DiscriminatorEntitySubclass child = new DiscriminatorEntitySubclass( 2, "child", root );
		s.persist( child );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		// in select clause
		List result = s.createQuery( "select e from DiscriminatorEntity e", Object[].class ).list();
		assertEquals( 2, result.size() );
		result = s.createQuery( "select treat (e as DiscriminatorEntitySubclass) from DiscriminatorEntity e", Object[].class ).list();
		assertEquals( 1, result.size() );
		result = s.createQuery( "select treat (e as DiscriminatorEntitySubSubclass) from DiscriminatorEntity e", Object[].class ).list();
		assertEquals( 0, result.size() );

		// in join
		result = s.createQuery( "from DiscriminatorEntity e inner join e.other", DiscriminatorEntity.class ).list();
		assertEquals( 1, result.size() );
		result = s.createQuery( "from DiscriminatorEntity e inner join treat (e.other as DiscriminatorEntitySubclass)", DiscriminatorEntity.class ).list();
		assertEquals( 0, result.size() );
		result = s.createQuery( "from DiscriminatorEntity e inner join treat (e.other as DiscriminatorEntitySubSubclass)", DiscriminatorEntity.class ).list();
		assertEquals( 0, result.size() );

		s.close();

		s = openSession();
		s.beginTransaction();
		s.remove( root );
		s.remove( child );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKey( value = "HHH-8637" )
	public void testFilteringJoinedSubclasses() {
		Session s = openSession();
		s.beginTransaction();
		JoinedEntity root = new JoinedEntity( 1, "root" );
		s.persist( root );
		JoinedEntitySubclass child = new JoinedEntitySubclass( 2, "child", root );
		s.persist( child );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

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

		s.close();

		s = openSession();
		s.beginTransaction();
		s.remove( child );
		s.remove( root );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKey( value = "HHH-9862" )
	public void testRestrictionsOnJoinedSubclasses() {
		Session s = openSession();
		s.beginTransaction();
		JoinedEntity root = new JoinedEntity( 1, "root" );
		s.persist( root );
		JoinedEntitySubclass child1 = new JoinedEntitySubclass( 2, "child1", root );
		s.persist( child1 );
		JoinedEntitySubclass2 child2 = new JoinedEntitySubclass2( 3, "child2", root );
		s.persist( child2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

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

		s.close();

		s = openSession();
		s.beginTransaction();
		s.remove( child1 );
		s.remove( child2 );
		s.remove( root );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKey(value = "HHH-9411")
	public void testTreatWithRestrictionOnAbstractClass() {
		inTransaction(
				s -> {
					Greyhound greyhound = new Greyhound();
					Dachshund dachshund = new Dachshund();
					s.persist( greyhound );
					s.persist( dachshund );

					List results = s.createQuery( "select treat (a as Dog) from Animal a where a.fast = TRUE" ).list();

					assertEquals( Arrays.asList( greyhound ), results );
					s.remove( greyhound );
					s.remove( dachshund );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16657")
	public void testTypeFilterInSubquery() {
		inTransaction(
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
		inSession(
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
		inTransaction(
				s -> {
					s.createMutationQuery( "update JoinedEntity j set j.other = null" ).executeUpdate();
					s.createMutationQuery( "delete from JoinedEntity" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16658")
	public void testPropagateEntityNameUsesFromDisjunction() {
		inSession(
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
	public void testPropagateEntityNameUsesFromDisjunction2() {
		inSession(
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
	public void testTreatInSelect() {
		inTransaction(
				s -> {
					JoinedEntitySubclass root1 = new JoinedEntitySubclass(1, "root1");
					JoinedEntitySubSubclass root2 = new JoinedEntitySubSubclass(2, "root2");
					s.persist( root1 );
					s.persist( root2 );
				}
		);
		inSession(
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
		inTransaction(
				s -> {
					s.createMutationQuery( "delete from JoinedEntity" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16571") // Sort of related to that issue
	public void testJoinSubclassOneToMany() {
		// Originally, the FK for "others" used the primary key of the root table JoinedEntity
		// Since we didn't register an entity use, we wrongly pruned that table before.
		// This was fixed by letting the FK descriptor point to the primary key of JoinedEntitySubclass2,
		// i.e. the plural attribute declaring type, which has the nice benefit of saving us a join
		inSession(
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
