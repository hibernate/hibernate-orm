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
package org.hibernate.test.generated;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
@RequiresDialect( Oracle9iDialect.class )
public class PartiallyGeneratedComponentTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "generated/ComponentOwner.hbm.xml" };
	}

	@Test
	public void testPartialComponentGeneration() {
		ComponentOwner owner = new ComponentOwner( "initial" );
		Session s = openSession();
		s.beginTransaction();
		s.save( owner );
		s.getTransaction().commit();
		s.close();

		assertNotNull( "expecting insert value generation", owner.getComponent() );
		int previousValue = owner.getComponent().getGenerated();
		assertFalse( "expecting insert value generation", 0 == previousValue );

		s = openSession();
		s.beginTransaction();
		owner = ( ComponentOwner ) s.get( ComponentOwner.class, owner.getId() );
		assertEquals( "expecting insert value generation", previousValue, owner.getComponent().getGenerated() );
		owner.setName( "subsequent" );
		s.getTransaction().commit();
		s.close();

		assertNotNull( owner.getComponent() );
		previousValue = owner.getComponent().getGenerated();

		s = openSession();
		s.beginTransaction();
		owner = ( ComponentOwner ) s.get( ComponentOwner.class, owner.getId() );
		assertEquals( "expecting update value generation", previousValue, owner.getComponent().getGenerated() );
		s.delete( owner );
		s.getTransaction().commit();
		s.close();
	}
}
