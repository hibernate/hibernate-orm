/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.tool.schemacreation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Jonathan Bregler
 * @author Andrea Boriero
 */
public class HANAStringTypeNVarcharCreationTest extends AbstractHANAStringAndBooleanFieldsCreationTest {
	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		super.applySettings( serviceRegistryBuilder );
		serviceRegistryBuilder.applySetting( "hibernate.dialect.hana.use_unicode_string_types", "true" );
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-12302")
	public void testTargetScriptIsCreatedStringTypeNVarchar(SchemaScope scope) throws Exception {
		String fileContent = getSqlScriptOutputFileContent();
		Pattern fileContentPattern = Pattern.compile(
				"create( (column|row))? table test_entity \\(field nvarchar.+, b boolean.+, c nvarchar.+, lob nclob" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true )
		);
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {

		@Id
		private String field;

		private char c;

		@Lob
		private String lob;

		private boolean b;
	}

}
