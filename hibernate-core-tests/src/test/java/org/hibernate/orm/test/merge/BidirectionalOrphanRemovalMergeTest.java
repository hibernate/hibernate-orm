package org.hibernate.orm.test.merge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@JiraKey("HHH-20973")
@DomainModel(
		annotatedClasses = {
				BidirectionalOrphanRemovalMergeTest.Parent.class,
				BidirectionalOrphanRemovalMergeTest.Child.class
		}
)
@SessionFactory
@Jpa(
		annotatedClasses = {
				BidirectionalOrphanRemovalMergeTest.Parent.class,
				BidirectionalOrphanRemovalMergeTest.Child.class
		}
)
class BidirectionalOrphanRemovalMergeTest {

	@Test
	void testPersistClearMerge(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child = new Child();
			child.parent = parent;
			parent.children.add( child );

			session.persist( parent );

			parent.children.clear();

			session.merge( parent );

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.children ).isEmpty();
		} );
	}

	@Test
	void testPersistMergeClear(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child = new Child();
			child.parent = parent;
			parent.children.add( child );

			session.persist( parent );

			session.merge( parent );

			parent.children.clear();

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.children ).isEmpty();
		} );
	}

	@Test
	void testPersistClearAddMerge(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child1 = new Child();
			child1.parent = parent;
			parent.children.add( child1 );

			session.persist( parent );

			parent.children.clear();

			Child child2 = new Child();
			child2.parent = parent;
			parent.children.add( child2 );

			Child child3 = new Child();
			child3.parent = parent;
			parent.children.add( child3 );

			session.merge( parent );

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.children ).hasSize(2);
		} );
	}

	@Test
	void testPersistMergeClearAdd(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child1 = new Child();
			child1.parent = parent;
			parent.children.add( child1 );

			session.persist( parent );

			session.merge( parent );

			parent.children.clear();

			Child child2 = new Child();
			child2.parent = parent;
			parent.children.add( child2 );

			Child child3 = new Child();
			child3.parent = parent;
			parent.children.add( child3 );

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.children ).hasSize(2);
		} );
	}

	@Test
	void testPersistFlushClearMerge(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child = new Child();
			child.parent = parent;
			parent.children.add( child );

			session.persist( parent );
			session.flush();

			parent.children.clear();

			session.merge( parent );

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.children ).isEmpty();
		} );
	}

	@Test
	void testPersistFlushMergeClear(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child = new Child();
			child.parent = parent;
			parent.children.add( child );

			session.persist( parent );
			session.flush();

			session.merge( parent );

			parent.children.clear();

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.children ).isEmpty();
		} );
	}

	@Test
	void testPersistFlushClearAddMerge(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child = new Child();
			child.parent = parent;
			parent.children.add( child );

			session.persist( parent );
			session.flush();

			parent.children.clear();

			Child child2 = new Child();
			child2.parent = parent;
			parent.children.add( child2 );

			Child child3 = new Child();
			child3.parent = parent;
			parent.children.add( child3 );

			session.merge( parent );

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.children ).hasSize(2);
		} );
	}

	@Test
	void testPersistFlushClearMergeAdd(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child = new Child();
			child.parent = parent;
			parent.children.add( child );

			session.persist( parent );
			session.flush();

			parent.children.clear();

			session.merge( parent );

			Child child2 = new Child();
			child2.parent = parent;
			parent.children.add( child2 );

			Child child3 = new Child();
			child3.parent = parent;
			parent.children.add( child3 );

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.children ).hasSize(2);
		} );
	}

	@JiraKey("HHH-20670")
	@Test
	void testPersistCommitClearMergeFlushJpa(EntityManagerFactoryScope scope) {
		Long parentId = scope.fromEntityManager(entityManager -> {
			Parent parent = new Parent();
			Child child = new Child();
			child.parent = parent;
			parent.children.add(child);

			entityManager.getTransaction().begin();
			entityManager.persist(parent);
			entityManager.getTransaction().commit();

			entityManager.getTransaction().begin();
			parent.children.clear();
			entityManager.merge(parent);
			entityManager.flush();
			entityManager.getTransaction().commit();

			return parent.id;
		});

		scope.inTransaction( entityManager -> {
			Parent parent = entityManager.find( Parent.class, parentId );
			assertThat( parent.children ).isEmpty();
		} );
	}

	@JiraKey("HHH-20670")
	@Test
	void testPersistCommitClearMergeFlush(SessionFactoryScope scope) {
		Long parentId = scope.fromSession(session -> {
			Parent parent = new Parent();
			Child child = new Child();
			child.parent = parent;
			parent.children.add(child);

			session.getTransaction().begin();
			session.persist(parent);
			session.getTransaction().commit();

			session.getTransaction().begin();
			parent.children.clear();
			session.merge(parent);
			session.flush();
			session.getTransaction().commit();

			return parent.id;
		});

		scope.inTransaction( entityManager -> {
			Parent parent = entityManager.find( Parent.class, parentId );
			assertThat( parent.children ).isEmpty();
		} );
	}

	@Entity(name = "parent")
	public static class Parent {

		@Id
		@GeneratedValue
		Long id;

		@OneToMany(
				mappedBy = "parent",
				cascade = CascadeType.ALL,
				fetch = FetchType.LAZY,
				orphanRemoval = true
		)
		List<Child> children = new ArrayList<>();

	}

	@Entity(name = "child")
	static class Child {

		@Id
		@GeneratedValue
		Long id;

		@ManyToOne
		Parent parent;

		@Override
		public String toString() {
			return "Child: id=" + id;
		}
	}
}
