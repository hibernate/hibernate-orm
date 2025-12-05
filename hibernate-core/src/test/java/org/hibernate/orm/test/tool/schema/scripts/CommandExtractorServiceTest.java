/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_IMPORT_FILES;

/**
 * @author Lukasz Antoniak
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-2403")
@JiraKey("HHH-6286")
@RequiresDialect(value = H2Dialect.class,
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
@ServiceRegistry(
		services = @ServiceRegistry.Service(
				role = SqlScriptCommandExtractor.class,
				impl = MultiLineSqlScriptExtractor.class
		),
		settings = @Setting(
				name = HBM2DDL_IMPORT_FILES,
				value = "/org/hibernate/orm/test/tool/schema/scripts/multi-line-statements.sql"
		)
)
@DomainModel(xmlMappings = "/org/hibernate/orm/test/tool/schema/scripts/Human.hbm.xml")
@SessionFactory
public class CommandExtractorServiceTest {
	@Test
	public void testImportFile(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			final Long count = session.createQuery( "select count(h.id) from Human h", Long.class ).uniqueResult();
			Assertions.assertEquals( 3L, count.longValue(), "Incorrect row count" );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}
}
