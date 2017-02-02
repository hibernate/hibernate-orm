/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SchemaUpdateHaltOnErrorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			From.class
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "update" );
		settings.put( AvailableSettings.HBM2DDL_HALT_ON_ERROR, true );
		return settings;
	}

	@Override
	public void buildEntityManagerFactory() throws Exception {
		try {
			super.buildEntityManagerFactory();
			fail("Should halt on error!");
		}
		catch ( Exception e ) {
			SchemaManagementException cause = (SchemaManagementException) e.getCause();
			assertEquals("Halting on error : Error executing DDL via JDBC Statement", cause.getMessage());
		}
	}

	@Test
	public void testHaltOnError() {
	}

	@Entity(name = "From")
	public class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}
