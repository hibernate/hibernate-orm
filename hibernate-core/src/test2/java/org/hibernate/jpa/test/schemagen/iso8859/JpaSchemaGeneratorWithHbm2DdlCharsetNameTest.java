/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen.iso8859;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.schemagen.JpaSchemaGeneratorTest;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( H2Dialect.class )
public class JpaSchemaGeneratorWithHbm2DdlCharsetNameTest
		extends JpaSchemaGeneratorTest {

	public String getScriptFolderPath() {
		return "org/hibernate/jpa/test/schemagen/iso8859/";
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put( AvailableSettings.HBM2DDL_CHARSET_NAME, "ISO-8859-1" );
		return settings;
	}
}