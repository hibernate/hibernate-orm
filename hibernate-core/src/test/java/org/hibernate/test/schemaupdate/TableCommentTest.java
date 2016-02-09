/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class TableCommentTest extends BaseNonConfigCoreFunctionalTestCase {

	private File output;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
	}

	@After
	public void tearDown() {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10451")
	public void testCommentOnTableStatementIsGenerated() throws IOException {
		createSchema( new Class[] {TableWithComment.class} );

		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
		boolean found = false;
		final String tableName = getTableName();
		for ( String sqlStatement : sqlLines ) {
			if ( sqlStatement.toLowerCase()
					.equals( "comment on table " + tableName.toLowerCase() + " is 'comment snippet'" ) ) {
				if ( getDialect().supportsCommentOn() ) {
					found = true;
				}
				else {
					fail( "Generated " + sqlStatement + "  statement, but Dialect does not support it" );
				}
			}
			if ( containsCommentInCreateTableStatement( sqlStatement ) ) {
				if ( getDialect().supportsCommentOn() && !getDialect().getTableComment( "comment snippet" ).equals( "" ) ) {
					fail( "Added comment on create table statement when Dialect support create comment on table statement" );
				}
				else {
					found = true;
				}
			}
		}

		assertThat( "Table Comment Statement not correctly generated", found, is( true ) );
	}

	private boolean containsCommentInCreateTableStatement(String sqlStatement) {
		return sqlStatement.toLowerCase().contains( "create table" ) && sqlStatement.toLowerCase()
				.contains( getDialect().getTableComment( "comment snippet" )
								   .toLowerCase() );
	}

	@Entity(name = "TableWithComment")
	@javax.persistence.Table(name = "TABLE_WITH_COMMENT")
	@Table(appliesTo = "TABLE_WITH_COMMENT", comment = "comment snippet")
	public static class TableWithComment {

		@Id
		private int id;
	}

	private String getTableName() {
		SessionFactoryImplementor sessionFactoryImplementor = sessionFactory();
		ClassMetadata tableWithCommentMetadata = sessionFactoryImplementor.getClassMetadata( TableWithComment.class );
		return ((AbstractEntityPersister) tableWithCommentMetadata).getTableName();
	}

	private void createSchema(Class[] annotatedClasses) {
		new SchemaExport()
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata() );
	}

	protected void afterMetadataSourcesApplied(MetadataSources metadataSources) {
		metadataSources.addAnnotatedClass( TableWithComment.class );
	}

	@Override
	protected boolean createSchema() {
		return false;
	}
}
