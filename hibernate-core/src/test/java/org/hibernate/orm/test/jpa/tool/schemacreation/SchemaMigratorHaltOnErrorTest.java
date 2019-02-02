/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.tool.schemacreation;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 is far more resistant to the reserved keyword usage. See HHH-12832.")
public class SchemaMigratorHaltOnErrorTest extends EntityManagerFactoryBasedFunctionalTest {

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
	public EntityManagerFactory produceEntityManagerFactory() {
		try {
			EntityManagerFactory entityManagerFactory = super.produceEntityManagerFactory();

			fail( "Should halt on error!" );
		}
		catch (Exception e) {
			SchemaManagementException cause = (SchemaManagementException) e.getCause();
			assertTrue( cause.getMessage().startsWith( "Halting on error : Error executing DDL" ) );
			assertTrue( cause.getMessage().endsWith( "via JDBC Statement" ) );
		}
		// to avoid EntityManagerFactoryAccess.getDialect() throwing a NPE
		return Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				new HashMap()
		).build();
	}

	@Test
	public void testHaltOnError() {
		try {
			entityManagerFactory();
		}
		catch (Exception e) {

		}
	}

	@Entity(name = "From")
	public class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}
