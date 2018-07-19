/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(value = DB2Dialect.class, comment = "DB2 is far more resistant to the reserved keyword usage. See HHH-12832.")
public class SchemaMigratorHaltOnErrorTest extends BaseEntityManagerFunctionalTestCase {

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
	public void buildEntityManagerFactory() {
		try {
			super.buildEntityManagerFactory();
			fail("Should halt on error!");
		}
		catch ( Exception e ) {
			SchemaManagementException cause = (SchemaManagementException) e.getCause();
			assertTrue( cause.getMessage().startsWith( "Halting on error : Error executing DDL" ) );
			assertTrue( cause.getMessage().endsWith( "via JDBC Statement" ) );
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
