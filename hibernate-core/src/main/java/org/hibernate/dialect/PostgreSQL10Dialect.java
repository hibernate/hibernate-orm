/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQL10IdentityColumnSupport;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorPostgresSQLDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * An SQL dialect for Postgres 10 and later.
 */
public class PostgreSQL10Dialect extends PostgreSQL95Dialect {

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new PostgreSQL10IdentityColumnSupport();
	}

	/**
	 * An SQL Dialect for PostgreSQL 10 and later. Adds support for Partition table.
	 *
	 * @param tableTypesList
	 */
	@Override
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		super.augmentRecognizedTableTypes( tableTypesList );
		tableTypesList.add( "PARTITIONED TABLE" );
	}

	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData dbMetaData) throws SQLException {

		if ( dbMetaData != null ) {
			builder.applyIdentifierCasing( dbMetaData );
			builder.applyReservedWords( dbMetaData );
		}
		else {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		builder.applyReservedWords( AnsiSqlKeywords.INSTANCE.sql2003() );
		builder.applyReservedWords( getKeywords() );

		builder.setNameQualifierSupport( getNameQualifierSupport() );

		return builder.build();
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}


	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorPostgresSQLDatabaseImpl.INSTANCE;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select current_schema from sys.dummy";
	}
}
