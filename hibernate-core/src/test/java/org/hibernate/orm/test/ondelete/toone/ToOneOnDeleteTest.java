/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondelete.toone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				ToOneOnDeleteTest.Parent.class,
				ToOneOnDeleteTest.Child.class,
				ToOneOnDeleteTest.GrandChild.class
		}
)
@SessionFactory
public class ToOneOnDeleteTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
	public void testManyToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent();
					parent.id = 1L;
					session.persist( parent );

					Child child1 = new Child();
					child1.id = 1L;
					child1.parent = parent;
					session.persist( child1 );

					GrandChild grandChild11 = new GrandChild();
					grandChild11.id = 1L;
					grandChild11.parent = child1;
					session.persist( grandChild11 );

					Child child2 = new Child();
					child2.id = 2L;
					child2.parent = parent;
					session.persist( child2 );

					GrandChild grandChild21 = new GrandChild();
					grandChild21.id = 2L;
					grandChild21.parent = child2;
					session.persist( grandChild21 );

					GrandChild grandChild22 = new GrandChild();
					grandChild22.id = 3L;
					grandChild22.parent = child2;
					session.persist( grandChild22 );
				}
		);

		scope.inTransaction(
				session -> {
					assertNotNull( session.get(Child.class, 1L) );
					assertNotNull( session.get(Child.class, 2L) );
					assertNotNull( session.get(GrandChild.class, 2L) );
					assertNotNull( session.get(GrandChild.class, 3L) );
				}
		);
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, 1L );
					session.remove( parent );
				}
		);
		scope.inTransaction(
				session -> {
					assertNull( session.get(Child.class, 1L) );
					assertNull( session.get(Child.class, 2L) );
					assertNull( session.get(GrandChild.class, 2L) );
					assertNull( session.get(GrandChild.class, 3L) );
					assertNull( session.get( Parent.class, 1L ) );
				}
		);	}


	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		private String name;
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		@OnDelete(action = OnDeleteAction.CASCADE)
		private Parent parent;
	}

	@Entity(name = "GrandChild")
	public static class GrandChild {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		@OnDelete(action = OnDeleteAction.CASCADE)
		private Child parent;
	}
}
