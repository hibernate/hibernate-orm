/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
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
	@JiraKey(value = "HHH-10451")
	public void testCommentOnTableStatementIsGenerated() throws IOException {
		createSchema( new Class[] {TableWithComment.class} );

		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
		boolean found = false;
		final String tableName = getTableName();
		for ( String sqlStatement : sqlLines ) {
			if ( sqlStatement.toLowerCase()
					.equals( "comment on table " + tableName.toLowerCase() + " is 'comment snippet';" ) ) {
				if ( getDialect().supportsCommentOn() ) {
					found = true;
				}
				else {
					fail( "Generated " + sqlStatement + "  statement, but Dialect does not support it" );
				}
			}
			if ( containsCommentInCreateTableStatement( sqlStatement ) ) {
				if ( getDialect().supportsCommentOn() || getDialect().getTableComment( "comment snippet" ).isEmpty() ) {
					found = true;
				}
				else {
					fail( "Generated comment on create table statement, but Dialect does not support it" );
				}
			}
		}

		assertThat( "Table Comment Statement not correctly generated", found, is( true ) );
	}

	private boolean containsCommentInCreateTableStatement(String sqlStatement) {
		return sqlStatement.toLowerCase().contains( getDialect().getCreateTableString() )
				&& sqlStatement.toLowerCase().contains( getDialect().getTableComment( "comment snippet" ).toLowerCase() );
	}

	@Entity(name = "TableWithComment")
	@Table(name = "TABLE_WITH_COMMENT", comment = "comment snippet")
	public static class TableWithComment {

		@Id
		private int id;
	}

	private String getTableName() {
		final SessionFactoryImplementor sessionFactory = sessionFactory();
		final EntityMappingType entityMappingType = sessionFactory
				.getRuntimeMetamodels()
				.getEntityMappingType( TableWithComment.class );

		return ( (AbstractEntityPersister) entityMappingType ).getTableName();
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
