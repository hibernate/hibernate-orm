/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.uniqueconstraint;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class UniqueConstraintGenerationTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.HBM2DDL_AUTO, "none" );
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-11101")
	public void testUniqueConstraintIsGenerated(SchemaScope scope) throws Exception {
		scope.withSchemaExport( schemaExport ->
										schemaExport
												.create( EnumSet.of( TargetType.SCRIPT ) ) );

		if ( getDialect() instanceof DB2Dialect ) {
			checkCreateUniqueIndexIsGenerated( "test_entity_item", "item" );
		}
		else {
			checkUniqueConstraintIsGenerated( "test_entity_item", "item" );
		}

		checkUniqueConstraintIsGenerated( "test_entity_children", "child" );
	}

	private void checkUniqueConstraintIsGenerated(String tableName, String columnName) throws IOException {
		final String regex = "alter table " + tableName + " add constraint uk_(.)* unique \\(" + columnName + "\\)";

		assertThat(
				"The test_entity_children table unique constraint has not been generated",
				isRegexMatching( regex ), is( true )
		);
	}

	private void checkCreateUniqueIndexIsGenerated(String tableName, String columnName) throws IOException {
		String regex = "create unique index uk_(.)* on " + tableName + " \\(" + columnName + "\\)";

		assertThat(
				"The test_entity_item table unique constraint has not been generated",
				isRegexMatching( regex ), is( true )
		);
	}

	private boolean isRegexMatching(String regex) throws IOException {
		boolean matches = false;
		final String[] sqlGeneratedStatements = getSqlScriptOutputFileContent().toLowerCase()
				.split( System.lineSeparator() );
		final Pattern p = Pattern.compile( regex );
		for ( String statement : sqlGeneratedStatements ) {
			final Matcher matcher = p.matcher( statement );
			if ( matcher.matches() ) {
				matches = true;
			}
		}
		return matches;
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		@ManyToMany()
		@JoinTable(name = "test_entity_children",
				joinColumns = @JoinColumn(name = "entity_id"),
				inverseJoinColumns = @JoinColumn(name = "child", unique = true))
		@OrderColumn(name = "order")
		private List<TestEntity> children;

		@ElementCollection
		@CollectionTable(name = "test_entity_item")
		@Column(name = "item", unique = true)
		private Set<String> items;
	}
}
