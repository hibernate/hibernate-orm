/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-12271")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportDropConstraints.class)
public class ForeignKeyDropTest extends BaseSchemaUnitTestCase {
	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ParentEntity.class, ChildEntity.class };
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-11236")
	public void testForeignKeyDropIsCorrectlyGenerated(SchemaScope schemaScope) throws Exception {
		schemaScope.withSchemaDropper( null, schemaDropper ->
				schemaDropper.doDrop(
						this,
						getSourceDescriptor(),
						getDatabaseTargetDescriptor( EnumSet.of( TargetType.DATABASE ) )
				) );
		assertThat(
				"The ddl foreign key drop command has not been properly generated",
				checkDropForeignKeyConstraint( "CHILD_ENTITY" ),
				is( true )
		);
	}

	private boolean checkDropForeignKeyConstraint(String tableName) throws IOException {
		boolean matches = false;
		String regex = getDialect().getAlterTableString( tableName );
		regex += getDialect().getDropForeignKeyString();

		if ( getDialect().supportsIfExistsBeforeConstraintName() ) {
			regex += "if exists ";
		}
		regex += "fk(.)*";
		if ( getDialect().supportsIfExistsAfterConstraintName() ) {
			regex += " if exists";
		}

		return isMatching( matches, regex.toLowerCase() );
	}

	private boolean isMatching(boolean matches, String regex) throws IOException {
		Pattern p = Pattern.compile( regex );
		for ( String line : getSqlScriptOutputFileLines() ) {
			final Matcher matcher = p.matcher( line.toLowerCase() );
			if ( matcher.matches() ) {
				matches = true;
			}
		}
		return matches;
	}

	@Entity(name = "ParentEntity")
	@Table(name = "PARENT_ENTITY")
	public static class ParentEntity {
		@Id
		private Long id;

		@OneToMany
		@JoinColumn(name = "PARENT")
		Set<ChildEntity> children;
	}

	@Entity(name = "ChildEntity")
	@Table(name = "CHILD_ENTITY")
	public static class ChildEntity {
		@Id
		private Long id;
	}
}
