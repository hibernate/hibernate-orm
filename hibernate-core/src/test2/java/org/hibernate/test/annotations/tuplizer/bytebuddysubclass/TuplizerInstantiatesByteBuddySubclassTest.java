/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.tuplizer.bytebuddysubclass;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.Tuplizer;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Florian Bien
 */
public class TuplizerInstantiatesByteBuddySubclassTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Test
	public void hhh11655Test() throws Exception {
		Session session = openSession();
		session.beginTransaction();

		SimpleEntity simpleEntityNonProxy = new SimpleEntity();
		Assert.assertFalse( session.contains( simpleEntityNonProxy ) );

		SimpleEntity simpleEntity = MyEntityInstantiator.createInstance( SimpleEntity.class );
		Assert.assertFalse( session.contains( simpleEntity ) );

		session.persist( simpleEntity );
		Assert.assertTrue( session.contains( simpleEntity ) );

		session.getTransaction().rollback();
		session.close();
	}

	@Entity(name = "SimpleEntity")
	@Tuplizer(impl = MyTuplizer.class)
	public static class SimpleEntity {
		protected SimpleEntity() {
		}

		@Id
		@GeneratedValue
		private Long id;

	}
}