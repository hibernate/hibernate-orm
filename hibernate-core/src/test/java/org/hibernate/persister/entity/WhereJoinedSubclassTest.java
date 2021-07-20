/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Where;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Daria Mitrofanova
 */
public class WhereJoinedSubclassTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ChildEntity.class, ParentEntity.class
		};
	}

	@Test
	public void testSchemaNotReplacedInCustomSQL() throws Exception {

		String className = ChildEntity.class.getName();

		final AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory().getEntityPersister(
				className );
		assertEquals( "( alias.id = 1) ", persister.getSQLWhereString( "alias" ) );
		assertEquals( "( alias.age = 1) ", persister.getSqlJoinedSubclassWhereString( "alias" ) );
		assertEquals( " and ( alias_1_.id = 1)  and ( alias.age = 1) ", persister.filterFragment( "alias" ) );

	}

	@Entity(name = "ChildEntity")
	@Persister(impl = JoinedSubclassEntityPersister.class)
	@Where(clause = "age = 1")
	public static class ChildEntity extends ParentEntity {

		private Integer age;
	}

	@Entity(name = "ParentEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Where(clause = "id = 1")
	public static class ParentEntity {
		@Id
		public Integer id;

		private String name;
	}
}
