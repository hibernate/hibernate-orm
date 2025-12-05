/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen.iso8859;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.schemagen.JpaSchemaGeneratorTest;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Vlad MIhalcea
 */
@RequiresDialect( H2Dialect.class )
@JiraKey( value = "HHH-10972" )
public class JpaFileSchemaGeneratorWithHbm2DdlCharsetNameTest extends JpaSchemaGeneratorTest {

	@Override
	public String getScriptFolderPath() {
		return super.getScriptFolderPath() + "iso8859/";
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put( AvailableSettings.HBM2DDL_CHARSET_NAME, "ISO-8859-1" );
		return settings;
	}

	protected String getLoadSqlScript() {
		return toFilePath(super.getLoadSqlScript());
	}

	protected String getCreateSqlScript() {
		return toFilePath(super.getCreateSqlScript());
	}

	protected String getDropSqlScript() {
		return toFilePath(super.getDropSqlScript());
	}

	@Override
	protected String getResourceUrlString(String resource) {
		return resource;
	}
}
