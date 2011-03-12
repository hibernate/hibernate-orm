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

package org.hibernate.envers.test.integration.reventity;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.integration.inheritance.joined.ChildEntity;
import org.hibernate.envers.test.integration.inheritance.joined.ParentEntity;
import org.hibernate.mapping.Column;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import java.util.Iterator;

/**
 * A join-inheritance test using a custom revision entity where the revision number is a long, mapped in the database
 * as an int.
 * @author Adam Warski (adam at warski dot org)
 */
public class LongRevEntityInheritanceChildAuditing extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(LongRevNumberRevEntity.class);
        cfg.addAnnotatedClass(ChildEntity.class);
        cfg.addAnnotatedClass(ParentEntity.class);
    }

    @Test
    public void testChildRevColumnType() {
        // We need the second column
        Iterator childEntityKeyColumnsIterator = getCfg()
                .getClassMapping("org.hibernate.envers.test.integration.inheritance.joined.ChildEntity_AUD")
                .getKey()
                .getColumnIterator();
        childEntityKeyColumnsIterator.next();
        Column second = (Column) childEntityKeyColumnsIterator.next();

        assertEquals(second.getSqlType(), "int");
    }
}