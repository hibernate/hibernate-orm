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
import org.hibernate.testing.TestForIssue;

/**
 * @author Vlad MIhalcea
 */
@RequiresDialect( H2Dialect.class )
@TestForIssue( jiraKey = "HHH-10972" )
public class JpaFileSchemaGeneratorWithHbm2DdlCharsetNameTest extends JpaSchemaGeneratorTest {

	public String getScriptFolderPath() {
		return "org/hibernate/jpa/test/schemagen/iso8859/";
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

	protected String toFilePath(String relativePath) {
		return Thread.currentThread().getContextClassLoader().getResource( relativePath ).getFile();
	}

	@Override
	protected String getResourceUrlString(String resource) {
		return resource;
	}
}