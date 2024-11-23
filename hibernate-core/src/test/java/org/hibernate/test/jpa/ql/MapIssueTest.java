/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.MapContent;
import org.hibernate.test.jpa.MapOwner;
import org.hibernate.test.jpa.Relationship;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-14279")
public class MapIssueTest extends AbstractJPATest {

	@Override
	public String[] getMappings() {
		return new String[] {};
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MapOwner.class, MapContent.class, Relationship.class};
	}

	@Test
	@RequiresDialect(value = PostgreSQL81Dialect.class, comment = "Requires support for using a correlated column in a join condition which H2 apparently does not support.")
	public void testWhereSubqueryMapKeyIsEntityWhereWithKey() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "select r from Relationship r where exists (select 1 from MapOwner as o left join o.contents c with key(c) = r)" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMapKeyIsEntityWhereWithKey() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "select 1 from MapOwner as o left join o.contents c where c.id is not null" ).list();
		s.getTransaction().commit();
		s.close();
	}
}
