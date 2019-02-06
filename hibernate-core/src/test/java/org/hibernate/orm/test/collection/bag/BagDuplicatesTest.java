/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BagDuplicatesTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting( AvailableSettings.SHOW_SQL, "true" );
	}

	@Test
	public void HHH10385Test() {
		Long parentId = inTransaction(
				session -> {
					Parent parent = new Parent();
					session.persist( parent );
					session.flush();
					return parent.getId();
				}
		);

		inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, parentId );
					Child child1 = new Child();
					child1.setName( "child1" );
					child1.setParent( parent );
					parent.addChild( child1 );
					session.merge( parent );
					session.flush();
					//assertEquals(1, parent.getChildren().size());
				}
		);


		inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, parentId );
					assertEquals( 1, parent.getChildren().size() );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
		private List<Child> children = new ArrayList<Child>();

		public Parent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
		}

		public void removeChild(Child child) {
			children.remove( child );
			child.setParent( null );
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return "Child{" +
					"id=" + id +
					", name='" + name + '\'' +
					'}';
		}
	}

}
