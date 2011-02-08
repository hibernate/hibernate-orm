/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.naturalid.immutable;
import java.lang.reflect.Field;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit.functional.FunctionalTestCase;

/**
 * @author Alex Burgel
 */
public class ImmutableEntityNaturalIdTest extends FunctionalTestCase {

    public ImmutableEntityNaturalIdTest(String str) {
        super(str);
    }

	public void testNaturalIdCheck() throws Exception {
        Session s = openSession();
        Transaction t = s.beginTransaction();
        Parent p = new Parent("alex");
        Child c = new Child("billy", p);
        s.persist(p);
        s.persist(c);
		t.commit();
		s.close();

        Field name = c.getClass().getDeclaredField("name");
        name.setAccessible(true);
        name.set(c, "phil");

		s = openSession();
		t = s.beginTransaction();
        try {
            s.saveOrUpdate( c );
			s.flush();
            fail( "should have failed because immutable natural ID was altered");
        }
        catch (HibernateException he) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}

        name.set(c, "billy");

		s = openSession();
		t = s.beginTransaction();
        s.delete(c);
        s.delete(p);
        t.commit();
        s.close();
    }

    public void testSaveParentWithDetachedChildren() throws Exception {
        Session s = openSession();
        Transaction t = s.beginTransaction();

        Parent p = new Parent("alex");
        Child c = new Child("billy", p);

        s.persist(p);
        s.persist(c);
        t.commit();
        s.close();

        s = openSession();
        t = s.beginTransaction();

        p = (Parent) s.createCriteria(Parent.class)
				.add( Restrictions.eq("name", "alex") )
				.setFetchMode("children", FetchMode.JOIN)
        .setCacheable(true)
        .uniqueResult();

        t.commit();
        s.close();

        s = openSession();
        t = s.beginTransaction();

        Child c2 = new Child("joey", p);
        p.getChildren().add(c2);

        s.update(p);

        // this fails if AbstractEntityPersister returns identifiers instead of entities from
        // AbstractEntityPersister.getNaturalIdSnapshot()
        s.flush();

        s.delete(p);
        t.commit();
        s.close();
    }

    public void configure(Configuration cfg) {
        cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
        cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
        cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
    }

    public String[] getMappings() {
        return new String[] { "naturalid/immutable/ParentChildWithManyToOne.hbm.xml" };
    }

    public static Test suite() {
        return new TestSuite( ImmutableEntityNaturalIdTest.class);
    }

}
