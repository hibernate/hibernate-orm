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
package org.hibernate.test.jpa.ql;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.test.jpa.AbstractJPATest;

/**
 * Tests for various JPAQL compliance issues
 *
 * @author Steve Ebersole
 */
public class JPAQLComplianceTest extends AbstractJPATest {
	@Test
	public void testAliasNameSameAsUnqualifiedEntityName() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "select item from Item item" ).list();
		s.createQuery( "select item from Item item where item.name = 'a'" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIdentifierCaseSensitive() throws Exception {
		Session s = openSession( );
		// a control test (a user reported that the JPA 'case insensitivity' support
		// caused problems with the "discriminator resolution" code; unable to reproduce)...
		s.createQuery( "from MyEntity e where e.class = MySubclassEntity" );
		s.createQuery( "from MyEntity e where e.other.class = MySubclassEntity" );
		s.createQuery( "from MyEntity where other.class = MySubclassEntity" );

		s.createQuery( "select object(I) from Item i").list();
		s.close();
	}

	@Test
	public void testIdentifierCasesensitivityAndDuplicateFromElements() throws Exception {
		Session s = openSession();
		s.createQuery( "select e from MyEntity e where exists (select 1 from MyEntity e2 where e2.other.name  = 'something' and e2.other.other = e)" );
		s.close();
	}

	@Test
	public void testGeneratedSubquery() {
		Session s = openSession();
		s.createQuery( "select c FROM Item c WHERE c.parts IS EMPTY" ).list();
		s.close();
	}

	@Test
	public void testOrderByAlias() {
		Session s = openSession();
		s.createQuery( "select c.name as myname FROM Item c ORDER BY myname" ).list();
		s.createQuery( "select p.name as name, p.stockNumber as stockNo, p.unitPrice as uPrice FROM Part p ORDER BY name, abs( p.unitPrice ), stockNo" ).list();
		s.close();
	}	
}
