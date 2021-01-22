/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade.multilevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.jboss.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(annotatedClasses = {
		MultiLevelCascadeRegularIdBasedParentChildAssociationTest.Parent.class,
		MultiLevelCascadeRegularIdBasedParentChildAssociationTest.Child.class,
		MultiLevelCascadeRegularIdBasedParentChildAssociationTest.Skill.class,
		MultiLevelCascadeRegularIdBasedParentChildAssociationTest.Hobby.class
})
public class MultiLevelCascadeRegularIdBasedParentChildAssociationTest {

	private static final Logger log = Logger.getLogger( MultiLevelCascadeRegularIdBasedParentChildAssociationTest.class );

	@BeforeAll
	protected void initialize(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = new Parent();
					parent.id = 1L;

					Child child1 = new Child();
					child1.id = 1L;
					parent.addChild( child1 );

					Child child2 = new Child();
					child2.id = 2L;
					parent.addChild( child2 );

					Skill skill = new Skill();
					skill.id = 1L;
					child1.addSkill( skill );

					entityManager.persist( parent );
				}
		);
	}

	@AfterAll
	protected void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Child" ).executeUpdate();
					entityManager.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12291" )
	public void testHibernateDeleteEntityWithoutInitializingCollections(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent mainEntity = entityManager.find( Parent.class, 1L);

					assertNotNull(mainEntity);
					assertFalse(mainEntity.getChildren().isEmpty());

					Optional<Child> childToRemove = mainEntity.getChildren().stream()
							.filter(child -> Long.valueOf( 1L ).equals(child.id)).findFirst();
					childToRemove.ifPresent( mainEntity::removeChild );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Child> children = new ArrayList<>();

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			child.setParent( this);
			children.add(child);
		}

		public void removeChild(Child child) {
			child.setParent( null);
			children.remove(child);
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Hobby> hobbies = new ArrayList<>();

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Skill> skills = new ArrayList<>();

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent mainEntity) {
			this.parent = mainEntity;
		}

		public void addSkill(Skill skill) {
			skill.owner = this;
			skills.add(skill);
		}
	}

	@Entity(name = "Skill")
	public static class Skill {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Child owner;
	}

	@Entity(name = "Hobby")
	public static class Hobby {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Child owner;
	}
}
