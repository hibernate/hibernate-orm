/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@DomainModel(annotatedClasses = TableCommentTest.TableWithComment.class)
@SessionFactory(exportSchema = false)
public class TableCommentTest {

	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
	}

	@Test
	@JiraKey(value = "HHH-10451")
	public void testCommentOnTableStatementIsGenerated(
			DomainModelScope modelScope,
			SessionFactoryScope factoryScope,
			@TempDir File tmpDir) throws IOException {
		var output = new File( tmpDir, "update_script.sql" );

		createSchema( modelScope, output);

		var dialect = factoryScope.getSessionFactory().getJdbcServices().getDialect();

		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
		boolean found = false;
		final String tableName = getTableName( factoryScope );
		for ( String sqlStatement : sqlLines ) {
			if ( sqlStatement.toLowerCase()
					.equals( "comment on table " + tableName.toLowerCase() + " is 'comment snippet';" ) ) {
				if ( dialect.supportsCommentOn() ) {
					found = true;
				}
				else {
					Assertions.fail( "Generated " + sqlStatement + "  statement, but Dialect does not support it" );
				}
			}
			if ( containsCommentInCreateTableStatement( sqlStatement, dialect ) ) {
				if ( dialect.supportsCommentOn() || dialect.getTableComment( "comment snippet" ).isEmpty() ) {
					found = true;
				}
				else {
					Assertions.fail( "Generated comment on create table statement, but Dialect does not support it" );
				}
			}
		}

		MatcherAssert.assertThat( "Table Comment Statement not correctly generated", found, is( true ) );
	}

	private boolean containsCommentInCreateTableStatement(String sqlStatement, Dialect dialect) {
		return sqlStatement.toLowerCase().contains( dialect.getCreateTableString() )
				&& sqlStatement.toLowerCase().contains( dialect.getTableComment( "comment snippet" ).toLowerCase() );
	}

	@Entity(name = "TableWithComment")
	@Table(name = "TABLE_WITH_COMMENT", comment = "comment snippet")
	public static class TableWithComment {

		@Id
		private int id;
	}

	private String getTableName(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityMappingType entityMappingType = sessionFactory
				.getRuntimeMetamodels()
				.getEntityMappingType( TableWithComment.class );

		return ( (AbstractEntityPersister) entityMappingType ).getTableName();
	}

	private void createSchema(DomainModelScope modelScope, File output) {
		new SchemaExport()
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), modelScope.getDomainModel() );
	}
}
