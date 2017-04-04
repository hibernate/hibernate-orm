/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen.iso8859;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.schemagen.SchemaCreateDropUtf8WithoutHbm2DdlCharsetNameTest;

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
