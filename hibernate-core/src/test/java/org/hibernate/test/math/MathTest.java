/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.math;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
@RequiresDialect( value = { Oracle8iDialect.class, H2Dialect.class } )
public class MathTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[]{"math/Math.hbm.xml"};
	}
	
	@Test
	public void testBitAnd() {
		MathEntity me = new MathEntity();
		me.setValue( 5 );
		
		Session s = openSession();
		s.beginTransaction();
		Long id = (Long) s.save( me );
		s.getTransaction().commit();
		s.close();
		
		s = openSession();
		s.beginTransaction();
		int value1 = ((Integer) s.createQuery( "select bitand(m.value,0) from MathEntity m where m.id=" + id ).uniqueResult()).intValue();
		int value2 = ((Integer) s.createQuery( "select bitand(m.value,2) from MathEntity m where m.id=" + id ).uniqueResult()).intValue();
		int value3 = ((Integer )s.createQuery( "select bitand(m.value,3) from MathEntity m where m.id=" + id ).uniqueResult()).intValue();
		s.getTransaction().commit();
		s.close();

		assertEquals(value1, 0);
		assertEquals(value2, 0);
		assertEquals(value3, 1);
	}

}
