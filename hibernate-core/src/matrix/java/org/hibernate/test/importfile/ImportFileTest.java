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
package org.hibernate.test.importfile;
import java.math.BigInteger;
import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ImportFileTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.HBM2DDL_IMPORT_FILES, "/humans.sql,/dogs.sql,/multiline-stmt.sql" );
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"importfile/Human.hbm.xml",
				"importfile/Dog.hbm.xml"
		};
	}

	@Test
	public void testSingleLineImportFile() throws Exception {
		Session s = openSession(  );
		final Transaction tx = s.beginTransaction();
		final List<?> humans = s.createQuery( "from " + Human.class.getName() ).list();
		assertEquals( "humans.sql not imported", 3, humans.size() );

		final List<?> dogs = s.createQuery( "from " + Dog.class.getName() ).list();
		assertEquals( "dogs.sql not imported", 3, dogs.size() );
		for (Object entity : dogs) {
			s.delete( entity );
		}
		for (Object entity : humans) {
			s.delete( entity );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testMultipleLineImportFile() throws Exception {
		Session s = openSession();
		final Transaction tx = s.beginTransaction();

		BigInteger count = (BigInteger) s.createSQLQuery( "SELECT COUNT(*) FROM test_data" ).uniqueResult();
		assertEquals( "incorrect row number", 3L, count.longValue() );

		String multilineText = (String) s.createSQLQuery( "SELECT text FROM test_data WHERE id = 2" ).uniqueResult();
		assertEquals( "multiline string inserted incorrectly", "Multiline comment line 1\r\n-- line 2'\r\n/* line 3 */", multilineText );

		String empty = (String) s.createSQLQuery( "SELECT text FROM test_data WHERE id = 3" ).uniqueResult();
		assertNull( "NULL value inserted incorrectly", empty );

		tx.commit();
		s.close();
	}
}
