/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.any;

import junit.framework.Assert;

import org.hibernate.Session;
import org.hibernate.internal.util.EntityPrinter;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Nikolay Shestakov
 */
public class AnyTypeEntityNameTest extends BaseCoreFunctionalTestCase {
    @Override
    public String[] getMappings() {
        return new String[] { "any/DBObject.hbm.xml" };
    }

    @Override
    public String getCacheConcurrencyStrategy() {
        // having second level cache causes a condition whereby the original test case would not fail...
        return null;
    }

    @Test
    public void testToLoggableString() {
        Session session = openSession();
        session.beginTransaction();
        DBObject obj1 = new DBObject("dbobject1");
        DBObjectReference ref = new DBObjectReference();
        ref.setRef(obj1);
        session.saveOrUpdate("dbobject1", obj1);
        session.saveOrUpdate(ref);
        session.getTransaction().commit();
        session.close();

        session = openSession();
        session.beginTransaction();
        ref = (DBObjectReference) session.load(DBObjectReference.class, ref.getId());
        String str = new EntityPrinter(sessionFactory()).toString(DBObjectReference.class.getName(), ref);
        Assert.assertEquals("org.hibernate.test.any.DBObjectReference{id=1, ref=dbobject1#1}", str);
        session.getTransaction().commit();
        session.close();

        session = openSession();
        session.beginTransaction();
        ref = (DBObjectReference) session.load(DBObjectReference.class, ref.getId());
        DBObject obj2 = new DBObject("dbobject2");
        ref.setRef(obj2);
        session.saveOrUpdate("dbobject2", obj2);
        session.getTransaction().commit();
        session.close();

        session = openSession();
        session.beginTransaction();
        ref = (DBObjectReference) session.load(DBObjectReference.class, ref.getId());
        str = new EntityPrinter(sessionFactory()).toString(DBObjectReference.class.getName(), ref);
        Assert.assertEquals("org.hibernate.test.any.DBObjectReference{id=1, ref=dbobject2#1}", str);
        session.getTransaction().commit();
        session.close();

        session = openSession();
        session.beginTransaction();
        session.delete(ref);
        session.delete("dbobject1", obj1);
        session.delete("dbobject2", obj2);
        session.getTransaction().commit();
        session.close();
    }
}
