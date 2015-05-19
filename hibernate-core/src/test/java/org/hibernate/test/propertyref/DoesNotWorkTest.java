/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class DoesNotWorkTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {DoesNotWork.class};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		configuration.setProperty( AvailableSettings.HBM2DDL_IMPORT_FILES, "/org/hibernate/test/propertyref/import.sql" );
	}

	@Test
	public void testIt() {
		DoesNotWorkPk pk = new DoesNotWorkPk();
		pk.setId1( "ZZZ" );
		pk.setId2( "00" );

//		{
//			Session session = openSession();
//			session.beginTransaction();
//			DoesNotWork entity = new DoesNotWork( pk );
//			entity.setGlobalNotes( Arrays.asList( "My first note!" ) );
//			session.save( entity );
//			session.getTransaction().commit();
//			session.close();
//		}

		{
			Session session = openSession();
			session.beginTransaction();
			DoesNotWork entity = (DoesNotWork) session.get( DoesNotWork.class, pk );
			List<String> notes = entity.getGlobalNotes();
			if ( notes != null && notes.size() > 0 ) {
				for ( String s : notes ) {
					System.out.println( s );
				}
			}
			session.delete( entity );
			session.getTransaction().commit();
			session.close();
		}
	}
}
