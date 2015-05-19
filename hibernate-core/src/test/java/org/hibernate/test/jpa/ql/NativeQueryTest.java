/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import org.hibernate.Session;

import org.hibernate.test.jpa.AbstractJPATest;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class NativeQueryTest extends AbstractJPATest {
	@Test
	public void testJpaStylePositionalParametersInNativeSql() {
		Session s = openSession();
		s.beginTransaction();
		s.createSQLQuery( "select NAME from EJB3_ITEM where ITEM_ID = ?1" ).setParameter( "1", new Long( 123 ) ).list();
		s.getTransaction().commit();
		s.close();
	}
}
