/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.propertyref;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@FailureExpectedWithNewMetamodel(
		message = "Thing is.. how does this not fail all the time?  That Session.get call should always return null, leading to NPE."
)
public class DoesNotWorkTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { DoesNotWork.class, DoesNotWorkPk.class };
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
