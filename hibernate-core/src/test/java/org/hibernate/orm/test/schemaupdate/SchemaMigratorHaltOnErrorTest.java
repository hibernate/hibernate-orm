/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(value = DB2Dialect.class, comment = "DB2 is far more resistant to the reserved keyword usage. See HHH-12832.")
@SkipForDialect(value = DerbyDialect.class, comment = "Derby is far more resistant to the reserved keyword usage.")
@SkipForDialect(value = FirebirdDialect.class, comment = "FirebirdDialect has autoQuoteKeywords enabled, so it is far more resistant to the reserved keyword usage.")
@SkipForDialect(value = AltibaseDialect.class, comment = "AltibaseDialect has autoQuoteKeywords enabled, so it is far more resistant to the reserved keyword usage.")
@SkipForDialect(value = InformixDialect.class, comment = "Informix is far more resistant to the reserved keyword usage.")
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
