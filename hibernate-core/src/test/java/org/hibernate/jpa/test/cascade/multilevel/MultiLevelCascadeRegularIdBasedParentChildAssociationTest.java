/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade.multilevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class MultiLevelCascadeRegularIdBasedParentChildAssociationTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( MultiLevelCascadeRegularIdBasedParentChildAssociationTest.class );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class,
				Skill.class,
				Hobby.class
		};
	}

	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12291" )
	public void testHibernateDeleteEntityWithoutInitializingCollections() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent mainEntity = entityManager.find( Parent.class, 1L);

			assertNotNull(mainEntity);
			assertFalse(mainEntity.getChildren().isEmpty());

			Optional<Child> childToRemove = mainEntity.getChildren().stream()
					.filter(child -> Long.valueOf( 1L ).equals(child.id)).findFirst();
			childToRemove.ifPresent( mainEntity::removeChild );
		} );
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
