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
package org.hibernate.test.onetomany;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
@FailureExpectedWithNewUnifiedXsd
public class OneToManyTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "onetomany/Parent.hbm.xml" };
	}

	@SuppressWarnings( {"unchecked", "UnusedAssignment"})
	@Test
    @SkipForDialect(
            value = CUBRIDDialect.class,
            comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                    "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
    )
	public void testOneToManyLinkTable() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Child c = new Child();
		c.setName("Child One");
		Parent p = new Parent();
		p.setName("Parent");
		p.getChildren().add(c);
		c.setParent(p);
		s.save(p);
		s.flush();
		
		p.getChildren().remove(c);
		c.setParent(null);
		s.flush();
		
		p.getChildren().add(c);
		c.setParent(p);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c.setParent(null);
		s.update(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c.setParent(p);
		s.update(c);
		t.commit();
		s.close();

		
		s = openSession();
		t = s.beginTransaction();
		c = (Child) s.createQuery("from Child").uniqueResult();
		s.createQuery("from Child c left join fetch c.parent").list();
		s.createQuery("from Child c inner join fetch c.parent").list();
		s.clear();
		p = (Parent) s.createQuery("from Parent p left join fetch p.children").uniqueResult();
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from Child").executeUpdate();
		s.createQuery("delete from Parent").executeUpdate();		
		t.commit();
		s.close();

	}

	@Test
	public void testManyToManySize() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		assertEquals( 0, s.createQuery("from Parent p where size(p.children) = 0").list().size() );
		assertEquals( 0, s.createQuery("from Parent p where p.children.size = 0").list().size() );
		t.commit();
		s.close();
	}

}

