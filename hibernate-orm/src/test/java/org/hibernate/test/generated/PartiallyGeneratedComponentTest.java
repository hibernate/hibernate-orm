/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
