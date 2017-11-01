/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.definition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.schemaupdate.BaseSchemaUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad MIhalcea
 */

public abstract class AbstractForeignKeyDefinitionTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return true;
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected abstract boolean validate(String fileContent);

	@Test
	@TestForIssue(jiraKey = "HHH-10643")
	public void testForeignKeyDefinitionOverridesDefaultNamingStrategy()
			throws Exception {

		createSchemaExport()
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ) );

		final String fileContent = getSqlScriptOutputFileContent();
		assertTrue( "Script file : " + fileContent, validate( fileContent ) );
	}

}
