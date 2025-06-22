/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Lukasz Antoniak
 */
@JiraKey( value = "HHH-2403" )
@RequiresDialect(value = H2Dialect.class,
		jiraKey = "HHH-6286",
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
public class CommandExtractorServiceTest extends MultiLineImportFileTest {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(
				Environment.HBM2DDL_IMPORT_FILES,
				"/org/hibernate/orm/test/tool/schema/scripts/multi-line-statements.sql"
		);
	}

	@Override
	protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		super.prepareBasicRegistryBuilder( serviceRegistryBuilder );
		serviceRegistryBuilder.addService( SqlScriptCommandExtractor.class, MultiLineSqlScriptExtractor.INSTANCE );
	}
}
