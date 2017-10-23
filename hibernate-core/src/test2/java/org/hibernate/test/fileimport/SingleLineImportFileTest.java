/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fileimport;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class SingleLineImportFileTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(
				Environment.HBM2DDL_IMPORT_FILES,
				"/org/hibernate/test/fileimport/humans.sql,/org/hibernate/test/fileimport/dogs.sql"
		);
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"fileimport/Human.hbm.xml",
				"fileimport/Dog.hbm.xml"
		};
	}

	@Test
	public void testImportFile() throws Exception {
		Session s = openSession();
		final Transaction tx = s.beginTransaction();
		final List<?> humans = s.createQuery( "from " + Human.class.getName() ).list();
		assertEquals( "humans.sql not imported", 3, humans.size() );

		final List<?> dogs = s.createQuery( "from " + Dog.class.getName() ).list();
		assertEquals( "dogs.sql not imported", 3, dogs.size() );
		for ( Object entity : dogs ) {
			s.delete( entity );
		}
		for ( Object entity : humans ) {
			s.delete( entity );
		}
		tx.commit();
		s.close();
	}
}
