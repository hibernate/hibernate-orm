/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.envers.test.integration.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NoneAudited extends BaseEnversJPAFunctionalTestCase {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity3.class);
    }

    @Test
    public void testRevisionInfoTableNotCreated() {
        @SuppressWarnings({"unchecked"}) List<PersistentClass> pcs = iteratorToList(getCfg().getClassMappings());
        Assert.assertEquals(1, pcs.size());
        Assert.assertTrue(pcs.get(0).getClassName().contains("BasicTestEntity3"));
    }

    private <T> List<T> iteratorToList(Iterator<T> it) {
        List<T> result = new ArrayList<T>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        return result;
    }
}