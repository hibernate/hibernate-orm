/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen.iso8859;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.schemagen.SchemaCreateDropUtf8WithoutHbm2DdlCharsetNameTest;

/**
 * @author Vlad Mihalcea
 */
public class SchemaCreateDropWithHbm2DdlCharsetNameTest extends
		SchemaCreateDropUtf8WithoutHbm2DdlCharsetNameTest {

	@Override
	protected Map getConfig() {
		Map settings = super.getConfig();
		settings.put( AvailableSettings.HBM2DDL_CHARSET_NAME, "ISO-8859-1" );
		return settings;
	}

	protected String expectedTableName() {
		return "test_" + '\uFFFD' + "ntity";
	}

	protected String expectedFieldName() {
		return "fi" + '\uFFFD' + "ld";
	}
}
