/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.inheritance.discriminator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * Originally from https://github.com/mkaletka/hibernate-test-case-templates/commit/2b3c075cacd07474d5565fa3bd5a6d0a48683dc0
 *
 * @author Christian Beikov
 */
public class MappedSuperclassExtendsEntityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TestEntity.class,
				GrandParent.class,
				Parent.class,
				Child1.class,
				Child2.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12332")
	public void testQueryingSingle() {
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "from TestEntity p" ).getResultList();
		} );
	}

	@Entity(name = "GrandParent")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@DiscriminatorColumn(name = "discriminator")
	public static abstract class GrandParent implements Serializable {
		private static final long serialVersionUID = 1L;

        @Id
        @GeneratedValue
		private Long id;

		public GrandParent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public static abstract class Parent extends GrandParent {

		@ManyToMany(mappedBy = "parents")
		private List<TestEntity> entities;

		public List<TestEntity> getEntities() {
			return entities;
		}

		public void setEntities(List<TestEntity> entities) {
			this.entities = entities;
		}

	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private Long id;
		@ManyToMany
		private List<GrandParent> parents;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<GrandParent> getParents() {
			return parents;
		}

		public void setParents(List<GrandParent> parents) {
			this.parents = parents;
		}

	}

	@Entity(name = "Child1")
	@DiscriminatorValue("CHILD1")
	public static class Child1 extends Parent {
	}

	@Entity(name = "Child2")
	@DiscriminatorValue("CHILD2")
	public static class Child2 extends Parent {
	}
}
