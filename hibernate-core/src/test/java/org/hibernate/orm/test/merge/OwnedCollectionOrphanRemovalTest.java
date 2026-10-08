package org.hibernate.orm.test.merge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
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
				OwnedCollectionOrphanRemovalTest.Parent.class,
				OwnedCollectionOrphanRemovalTest.Child.class
		}
)
@SessionFactory
class OwnedCollectionOrphanRemovalTest {

	@Test
	void testPersistClearMerge(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child = new Child();
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
	void testPersistFlushClearMerge(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();

			Child child = new Child();
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

	@Entity(name = "parent")
	public static class Parent {

		@Id
		@GeneratedValue
		Long id;

		@OneToMany(
				cascade = CascadeType.ALL,
				fetch = FetchType.LAZY,
				orphanRemoval = true
		)
		List<Child> children = new ArrayList<>();

	}

	@Entity(name = "child")
	public static class Child {

		@Id
		@GeneratedValue
		Long id;

		@ManyToOne
		Parent parent;

	}
}
