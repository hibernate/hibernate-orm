/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.where;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Max Rydahl Andersen
 */
public class WhereTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "where/File.hbm.xml" };
	}

	@Test
	public void testWhere() {
		Session s = openSession();
		s.getTransaction().begin();
		File parent = new File("parent", null);
		s.persist( parent );
		s.persist( new File("child", parent) );
		File deletedChild = new File("deleted child", parent);
		deletedChild.setDeleted(true);
		s.persist( deletedChild );
		File deletedParent = new File("deleted parent", null);
		deletedParent.setDeleted(true);
		s.persist( deletedParent );
		s.flush();
		s.clear();
		parent = (File) s.createCriteria(File.class)
				.setFetchMode("children", FetchMode.JOIN)
				.add( Restrictions.isNull("parent") )
				.uniqueResult();
		assertEquals( parent.getChildren().size(), 1 );
		s.clear();
		parent = (File) s.createQuery("from File f left join fetch f.children where f.parent is null")
			.uniqueResult();
		assertEquals( parent.getChildren().size(), 1 );
		s.getTransaction().commit();
		s.close();
	}

}

