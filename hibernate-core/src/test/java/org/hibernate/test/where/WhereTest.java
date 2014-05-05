/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.where;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Max Rydahl Andersen
 */
@FailureExpectedWithNewUnifiedXsd(message = "New schema only defines where at the class level, not collections.")
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

