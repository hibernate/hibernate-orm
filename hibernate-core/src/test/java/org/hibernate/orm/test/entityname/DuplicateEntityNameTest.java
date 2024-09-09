/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.entityname;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.DuplicateMappingException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-13060" )
public class DuplicateEntityNameTest extends BaseCoreFunctionalTestCase {

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Purchase1.class,
				Purchase2.class
		};
	}

	@Override
	protected void buildSessionFactory() {
		try {
			super.buildSessionFactory();
			fail("Should throw DuplicateMappingException");
		}
		catch (DuplicateMappingException e) {
			assertEquals( "Entity classes [org.hibernate.orm.test.entityname.DuplicateEntityNameTest$Purchase1] and [org.hibernate.orm.test.entityname.DuplicateEntityNameTest$Purchase2] share the entity name 'Purchase' (entity names must be distinct)", e.getMessage() );
		}
	}

	@Test
	public void test() {

	}

	@Entity(name = "Purchase")
	@Table(name="purchase_old")
	public static class Purchase1 {
		@Id
		public String uid;
	}

	@Entity(name = "Purchase")
	@Table(name="purchase_new")
	public static class Purchase2 {
		@Id
		public String uid;
	}

}
