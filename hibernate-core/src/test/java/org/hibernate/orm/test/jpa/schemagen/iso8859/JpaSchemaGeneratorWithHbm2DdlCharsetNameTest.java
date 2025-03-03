/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen.iso8859;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.schemagen.JpaSchemaGeneratorTest;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( H2Dialect.class )
public class JpaSchemaGeneratorWithHbm2DdlCharsetNameTest
		extends JpaSchemaGeneratorTest {

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
}
