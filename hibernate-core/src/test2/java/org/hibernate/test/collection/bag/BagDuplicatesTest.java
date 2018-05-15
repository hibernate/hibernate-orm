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
package org.hibernate.test.collection.bag;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 *
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
public class BagDuplicatesTest extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			Parent.class,
			Child.class
		};
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[] {
//				"Foo.hbm.xml",
//				"Bar.hbm.xml"
		};
	}
	// If those mappings reside somewhere other than resources/org/hibernate/test, change this.
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, "true" );
	}

	// Add your tests, using standard JUnit.
	@Test
	public void HHH10385Test() throws Exception {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		Session session = null;
		Transaction transaction = null;

        Long parentId = null;

		try {
            session = openSession();
            transaction = session.beginTransaction();

            Parent parent = new Parent();
            session.persist(parent);
            session.flush();
            parentId = parent.getId();

			transaction.commit();
		} catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            fail(e.getMessage());
		} finally {
            if (session != null) {
                session.close();
            }
        }

        try {
            session = openSession();
            transaction = session.beginTransaction();

            Parent parent = session.get(Parent.class, parentId);
            Child child1 = new Child();
            child1.setName("child1");
            child1.setParent(parent);
            parent.addChild(child1);
            parent = (Parent) session.merge(parent);
            session.flush();
            //assertEquals(1, parent.getChildren().size());

            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            fail(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }

        try {
            session = openSession();
            transaction = session.beginTransaction();

            Parent parent = session.get(Parent.class, parentId);
            assertEquals(1, parent.getChildren().size());

            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            fail(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
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
			children.add(child);
			child.setParent(this);
		}

		public void removeChild(Child child) {
			children.remove(child);
			child.setParent(null);
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
