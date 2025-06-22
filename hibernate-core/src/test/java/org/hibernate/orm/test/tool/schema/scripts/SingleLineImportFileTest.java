/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class SingleLineImportFileTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(
				Environment.HBM2DDL_IMPORT_FILES,
				"/org/hibernate/orm/test/tool/schema/scripts/dogs.sql"
		);
	}

	@Override
	protected String getBaseForMappings() {
		return "";
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"/org/hibernate/orm/test/tool/schema/scripts/Human.hbm.xml",
				"/org/hibernate/orm/test/tool/schema/scripts/Dog.hbm.xml"
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

		s.createQuery( "delete Dog" ).executeUpdate();
		s.createQuery( "delete Human" ).executeUpdate();

		tx.commit();
		s.close();
	}
}
