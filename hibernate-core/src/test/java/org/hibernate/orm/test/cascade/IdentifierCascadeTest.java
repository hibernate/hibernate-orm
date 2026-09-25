package org.hibernate.orm.test.cascade;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.processing.Exclude;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		IdentifierCascadeTest.Parent.class,
		IdentifierCascadeTest.SimpleIdChild.class,
		IdentifierCascadeTest.IdClassParent.class,
		IdentifierCascadeTest.IdClassChild.class,
		IdentifierCascadeTest.EmbeddedIdChild.class,
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19930")
public class IdentifierCascadeTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	// --- @Id @ManyToOne(cascade = ALL) ---

	@Test
	public void testPersistOnIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );

			SimpleIdChild child = new SimpleIdChild();
			child.setParent( parent );

			session.persist( child );
			assertTrue( session.contains( parent ) );
		} );
		scope.inTransaction( session -> {
			assertNotNull( session.find( Parent.class, 1L ) );
		} );
	}

	@Test
	public void testRemoveOnIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );
			session.persist( parent );

			SimpleIdChild child = new SimpleIdChild();
			child.setParent( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			SimpleIdChild child = session.find( SimpleIdChild.class, 1L );
			assertNotNull( child );
			session.remove( child );
		} );
		scope.inTransaction( session -> {
			assertNull( session.find( Parent.class, 1L ) );
		} );
	}

	@Test
	public void testMergeOnIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );
			parent.setName( "original" );
			session.persist( parent );

			SimpleIdChild child = new SimpleIdChild();
			child.setParent( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			Parent detachedParent = new Parent();
			detachedParent.setId( 1L );
			detachedParent.setName( "updated" );

			SimpleIdChild detachedChild = new SimpleIdChild();
			detachedChild.setParent( detachedParent );

			SimpleIdChild merged = session.merge( detachedChild );
			assertEquals( "updated", merged.getParent().getName() );
		} );
		scope.inTransaction( session -> {
			assertEquals( "updated", session.find( Parent.class, 1L ).getName() );
		} );
	}

	@Test
	public void testRefreshOnIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );
			parent.setName( "original" );
			session.persist( parent );

			SimpleIdChild child = new SimpleIdChild();
			child.setParent( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			SimpleIdChild child = session.find( SimpleIdChild.class, 1L );
			session.createNativeQuery( "update Parent set name = 'updated' where id = :id" )
					.setParameter( "id", 1L )
					.executeUpdate();

			session.refresh( child );
			assertEquals( "updated", child.getParent().getName() );
		} );
	}

	@Test
	public void testDetachOnIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );
			session.persist( parent );

			SimpleIdChild child = new SimpleIdChild();
			child.setParent( parent );
			session.persist( child );
			session.flush();

			assertTrue( session.contains( parent ) );
			assertTrue( session.contains( child ) );

			session.detach( child );

			assertFalse( session.contains( child ) );
			assertFalse( session.contains( parent ) );
		} );
	}

	// --- @IdClass with @Id @ManyToOne(cascade = ALL) ---

	@Test
	public void testPersistOnIdClassManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IdClassParent parent = new IdClassParent();
			parent.setId( 1L );

			IdClassChild child = new IdClassChild();
			child.setChildId( 10L );
			child.setParent( parent );

			session.persist( child );
			assertTrue( session.contains( parent ) );
		} );
		scope.inTransaction( session -> {
			assertNotNull( session.find( IdClassParent.class, 1L ) );
		} );
	}

	@Test
	public void testRemoveOnIdClassManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IdClassParent parent = new IdClassParent();
			parent.setId( 1L );
			session.persist( parent );

			IdClassChild child = new IdClassChild();
			child.setChildId( 10L );
			child.setParent( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			IdClassChild child = session.find(
					IdClassChild.class,
					new IdClassChildPK( 10L, 1L )
			);
			assertNotNull( child );
			session.remove( child );
		} );
		scope.inTransaction( session -> {
			assertNull( session.find( IdClassParent.class, 1L ) );
		} );
	}

	@Test
	public void testMergeOnIdClassManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IdClassParent parent = new IdClassParent();
			parent.setId( 1L );
			parent.setName( "original" );
			session.persist( parent );

			IdClassChild child = new IdClassChild();
			child.setChildId( 10L );
			child.setParent( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			IdClassParent detachedParent = new IdClassParent();
			detachedParent.setId( 1L );
			detachedParent.setName( "updated" );

			IdClassChild detachedChild = new IdClassChild();
			detachedChild.setChildId( 10L );
			detachedChild.setParent( detachedParent );

			IdClassChild merged = session.merge( detachedChild );
			assertEquals( "updated", merged.getParent().getName() );
		} );
		scope.inTransaction( session -> {
			assertEquals( "updated", session.find( IdClassParent.class, 1L ).getName() );
		} );
	}

	@Test
	public void testRefreshOnIdClassManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IdClassParent parent = new IdClassParent();
			parent.setId( 1L );
			parent.setName( "original" );
			session.persist( parent );

			IdClassChild child = new IdClassChild();
			child.setChildId( 10L );
			child.setParent( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			IdClassChild child = session.find(
					IdClassChild.class,
					new IdClassChildPK( 10L, 1L )
			);
			session.createNativeQuery( "update IdClassParent set name = 'updated' where id = :id" )
					.setParameter( "id", 1L )
					.executeUpdate();

			session.refresh( child );
			assertEquals( "updated", child.getParent().getName() );
		} );
	}

	@Test
	public void testDetachOnIdClassManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IdClassParent parent = new IdClassParent();
			parent.setId( 1L );
			session.persist( parent );

			IdClassChild child = new IdClassChild();
			child.setChildId( 10L );
			child.setParent( parent );
			session.persist( child );
			session.flush();

			assertTrue( session.contains( parent ) );
			assertTrue( session.contains( child ) );

			session.detach( child );

			assertFalse( session.contains( child ) );
			assertFalse( session.contains( parent ) );
		} );
	}

	// --- @EmbeddedId with @ManyToOne(cascade = ALL) ---

	@Test
	public void testPersistOnEmbeddedIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );

			EmbeddedIdChild child = new EmbeddedIdChild();
			child.setPk( new EmbeddedChildPK() );
			child.getPk().setChildId( 10L );
			child.getPk().setParent( parent );

			session.persist( child );
			assertTrue( session.contains( parent ) );
		} );
		scope.inTransaction( session -> {
			assertNotNull( session.find( Parent.class, 1L ) );
		} );
	}

	@Test
	public void testRemoveOnEmbeddedIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );
			session.persist( parent );

			EmbeddedIdChild child = new EmbeddedIdChild();
			child.setPk( new EmbeddedChildPK() );
			child.getPk().setChildId( 10L );
			child.getPk().setParent( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			EmbeddedChildPK pk = new EmbeddedChildPK();
			pk.setChildId( 10L );
			pk.setParent( session.getReference( Parent.class, 1L ) );
			EmbeddedIdChild child = session.find( EmbeddedIdChild.class, pk );
			assertNotNull( child );
			session.remove( child );
		} );
		scope.inTransaction( session -> {
			assertNull( session.find( Parent.class, 1L ) );
		} );
	}

	@Test
	public void testMergeOnEmbeddedIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );
			parent.setName( "original" );
			session.persist( parent );

			EmbeddedIdChild child = new EmbeddedIdChild();
			child.setPk( new EmbeddedChildPK() );
			child.getPk().setChildId( 10L );
			child.getPk().setParent( parent );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			Parent detachedParent = new Parent();
			detachedParent.setId( 1L );
			detachedParent.setName( "updated" );

			EmbeddedIdChild detachedChild = new EmbeddedIdChild();
			detachedChild.setPk( new EmbeddedChildPK() );
			detachedChild.getPk().setChildId( 10L );
			detachedChild.getPk().setParent( detachedParent );

			EmbeddedIdChild merged = session.merge( detachedChild );
			assertEquals( "updated", merged.getPk().getParent().getName() );
		} );
		scope.inTransaction( session -> {
			assertEquals( "updated", session.find( Parent.class, 1L ).getName() );
		} );
	}

	@Test
	public void testRefreshOnEmbeddedIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );
			parent.setName( "original" );
			session.persist( parent );
			Parent parent2 = new Parent();
			parent2.setId( 2L );
			parent2.setName( "original" );
			session.persist( parent2 );

			EmbeddedIdChild child = new EmbeddedIdChild();
			child.setPk( new EmbeddedChildPK() );
			child.getPk().setChildId( 10L );
			child.getPk().setParent( parent );
			child.setParent2( parent2 );
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			EmbeddedChildPK pk = new EmbeddedChildPK();
			pk.setChildId( 10L );
			pk.setParent( session.getReference( Parent.class, 1L ) );
			EmbeddedIdChild child = session.find( EmbeddedIdChild.class, pk );
			session.createNativeQuery( "update Parent set name = 'updated' where id = :id" )
					.setParameter( "id", 1L )
					.executeUpdate();
			session.createNativeQuery( "update Parent set name = 'updated' where id = :id" )
					.setParameter( "id", 2L )
					.executeUpdate();

			session.refresh( child );
			assertEquals( "updated", child.getPk().getParent().getName() );
			assertEquals( "updated", child.getParent2().getName() );
		} );
	}

	@Test
	public void testDetachOnEmbeddedIdManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.setId( 1L );
			session.persist( parent );

			EmbeddedIdChild child = new EmbeddedIdChild();
			child.setPk( new EmbeddedChildPK() );
			child.getPk().setChildId( 10L );
			child.getPk().setParent( parent );
			session.persist( child );
			session.flush();

			assertTrue( session.contains( parent ) );
			assertTrue( session.contains( child ) );

			session.detach( child );

			assertFalse( session.contains( child ) );
			assertFalse( session.contains( parent ) );
		} );
	}

	// --- Entity classes ---

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private long id;
		private String name;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Exclude
	@Entity(name = "SimpleIdChild")
	public static class SimpleIdChild {
		@Id
		@ManyToOne(cascade = CascadeType.ALL)
		private Parent parent;
		private String name;

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "IdClassParent")
	public static class IdClassParent {
		@Id
		private long id;
		private String name;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "IdClassChild")
	@IdClass(IdClassChildPK.class)
	public static class IdClassChild {
		@Id
		private long childId;

		@Id
		@ManyToOne(cascade = CascadeType.ALL)
		private IdClassParent parent;
		private String name;

		public long getChildId() {
			return childId;
		}

		public void setChildId(long childId) {
			this.childId = childId;
		}

		public IdClassParent getParent() {
			return parent;
		}

		public void setParent(IdClassParent parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class IdClassChildPK implements Serializable {
		private long childId;
		private long parent;

		public IdClassChildPK() {
		}

		public IdClassChildPK(long childId, long parent) {
			this.setChildId( childId );
			this.setParent( parent );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) return true;
			if ( !( o instanceof IdClassChildPK that ) ) return false;
			return getChildId() == that.getChildId() && getParent() == that.getParent();
		}

		@Override
		public int hashCode() {
			return Long.hashCode( getChildId() ) * 31 + Long.hashCode( getParent() );
		}

		public long getChildId() {
			return childId;
		}

		public void setChildId(long childId) {
			this.childId = childId;
		}

		public long getParent() {
			return parent;
		}

		public void setParent(long parent) {
			this.parent = parent;
		}
	}

	@Embeddable
	public static class EmbeddedChildPK implements Serializable {
		private long childId;

		@ManyToOne(cascade = CascadeType.ALL)
		private Parent parent;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) return true;
			if ( !( o instanceof EmbeddedChildPK that ) ) return false;
			return getChildId() == that.getChildId()
				&& (getParent() == that.getParent()
					|| getParent() != null && that.getParent() != null && getParent().getId() == that.getParent()
					.getId());
		}

		@Override
		public int hashCode() {
			return Long.hashCode( getChildId() );
		}

		public long getChildId() {
			return childId;
		}

		public void setChildId(long childId) {
			this.childId = childId;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "EmbeddedIdChild")
	public static class EmbeddedIdChild {
		@EmbeddedId
		private EmbeddedChildPK pk;
		private String name;
		@ManyToOne(cascade = CascadeType.ALL)
		private Parent parent2;

		public EmbeddedChildPK getPk() {
			return pk;
		}

		public void setPk(EmbeddedChildPK pk) {
			this.pk = pk;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent2() {
			return parent2;
		}

		public void setParent2(Parent parent2) {
			this.parent2 = parent2;
		}
	}
}
