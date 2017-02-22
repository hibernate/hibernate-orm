/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class NativeQueryDoesNotSupportIterationTest
		extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Test(expected = UnsupportedOperationException.class)
	public void iterateShouldThrowAnUnsupportedOperationException() {
		try (Session session = openSession();) {
			final NativeQuery sqlQuery = session.createNativeQuery(
					"select * from TEST_ENTITY" );
			sqlQuery.iterate();
		}
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {

		@Id
		public Long id;
	}
}
