/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.generated;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.BinaryType;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Implementation of AbstractGeneratedPropertyTest.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGeneratedPropertyTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-2627" )
	public final void testGeneratedProperty() {
		// The following block is repeated 300 times to reproduce HHH-2627.
		// Without the fix, Oracle will run out of cursors using 10g with
		// a default installation (ORA-01000: maximum open cursors exceeded).
		// The number of loops may need to be adjusted depending on the how
		// Oracle is configured.
		// Note: The block is not indented to avoid a lot of irrelevant differences.
		for ( int i=0; i<300; i++ ) {
			GeneratedPropertyEntity entity = new GeneratedPropertyEntity();
			entity.setName( "entity-1" );
			Session s = openSession();
			Transaction t = s.beginTransaction();
			s.save( entity );
			s.flush();
			assertNotNull( "no timestamp retrieved", entity.getLastModified() );
			t.commit();
			s.close();

			byte[] bytes = entity.getLastModified();

			s = openSession();
			t = s.beginTransaction();
			entity = ( GeneratedPropertyEntity ) s.get( GeneratedPropertyEntity.class, entity.getId() );
			assertTrue( BinaryType.INSTANCE.isEqual( bytes, entity.getLastModified() ) );
			t.commit();
			s.close();

			assertTrue( BinaryType.INSTANCE.isEqual( bytes, entity.getLastModified() ) );

			s = openSession();
			t = s.beginTransaction();
			s.delete( entity );
			t.commit();
			s.close();
		}
	}
}
