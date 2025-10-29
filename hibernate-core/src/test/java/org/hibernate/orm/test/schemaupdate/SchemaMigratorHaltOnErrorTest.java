/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_HALT_ON_ERROR;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SkipForDialect(dialectClass = DB2Dialect.class,
		matchSubTypes = true,
		reason = "DB2 is far more resistant to the reserved keyword usage. See HHH-12832.")
@SkipForDialect(dialectClass = DerbyDialect.class,
		reason = "Derby is far more resistant to the reserved keyword usage.")
@SkipForDialect(dialectClass = FirebirdDialect.class,
		reason = "FirebirdDialect has autoQuoteKeywords enabled, so it is far more resistant to the reserved keyword usage.")
@SkipForDialect(dialectClass = AltibaseDialect.class,
		reason = "AltibaseDialect has autoQuoteKeywords enabled, so it is far more resistant to the reserved keyword usage.")
@SkipForDialect(dialectClass = InformixDialect.class,
		reason = "Informix is far more resistant to the reserved keyword usage.")
@Jpa(
		annotatedClasses = SchemaMigratorHaltOnErrorTest.From.class,
		integrationSettings = {
				@Setting(name = HBM2DDL_AUTO, value = "update"),
				@Setting(name = HBM2DDL_HALT_ON_ERROR, value = "true")
		}
)
public class SchemaMigratorHaltOnErrorTest {
	@Test
	void testHaltOnError(EntityManagerFactoryScope factoryScope) {
		try {
			factoryScope.getEntityManagerFactory();
			Assertions.fail( "Expecting this to fail" );
		}
		catch (Exception e) {
			SchemaManagementException cause = (SchemaManagementException) e.getCause();
			Assertions.assertTrue( cause.getMessage().startsWith( "Halting on error : Error executing DDL" ) );
		}
	}

	@Entity(name = "From")
	public static class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}
