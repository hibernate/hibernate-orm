/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.SPI.Role.SUPPLY;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.function.DB2SubstringFunction;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.identity.internal.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.internal.DB2zIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.lock.internal.DB2LockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.spi.FetchLimitHandler;
import org.hibernate.dialect.pagination.spi.LegacyDB2LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.sequence.internal.DB2iSequenceSupport;
import org.hibernate.dialect.sequence.internal.NoSequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sql.ast.internal.DB2iSqlAstTranslator;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;


import static org.hibernate.type.SqlTypes.ROWID;

/**
 * A SQL dialect for DB2 for IBM i version 7.2 and above, previously known as "DB2/400".
 *
 * @author Peter DeGregorio (pdegregorio)
 * @author Christian Beikov
 */
public class DB2iDialect extends DB2Dialect {

	private final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 7, 2 );
	public final static DatabaseVersion DB2_LUW_VERSION = DB2Dialect.MINIMUM_VERSION;

	private static final String FOR_UPDATE_SQL = " for update with rs";
	private static final String FOR_UPDATE_SKIP_LOCKED_SQL = FOR_UPDATE_SQL + " skip locked data";
	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder(
					"select distinct sequence_schema as seqschema, sequence_name as seqname, START, minimum_value as minvalue, maximum_value as maxvalue, increment from qsys2.syssequences "
							+ "where current_schema='*LIBL' and sequence_schema in (select schema_name from qsys2.library_list_info) "
							+ "or sequence_schema=current_schema"
			)
					.sequenceNameColumn( "seqname" )
					.withoutCatalog()
					.schemaColumn( "seqschema" )
					.startValueColumn( "start" )
					.minimumValueColumn( "minvalue" )
					.maximumValueColumn( "maxvalue" )
					.build();

	public DB2iDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( MINIMUM_VERSION ) );
	}

	public DB2iDialect() {
		this( MINIMUM_VERSION );
	}

	public DB2iDialect(DatabaseVersion version) {
		super(version);
	}

	@Override
	protected LockingSupport buildLockingSupport() {
		return DB2LockingSupport.forDB2i();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		// DB2 for i doesn't allow code units: https://www.ibm.com/docs/en/i/7.1.0?topic=functions-substring
		functionContributions.getFunctionRegistry().register(
				"substring",
				new DB2SubstringFunction( false, functionContributions.getTypeConfiguration() )
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String createCommand(IndexDdlRequest request) {
		// we only create unique indexes, as opposed to unique constraints,
		// when the column is nullable, so safe to infer unique => nullable
		return request.unique() ? "create unique where not null index" : "create index";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String createTail(IndexDdlRequest request) {
		return "";
	}

	@Override
	public GeneratedValuesSupport getGeneratedValuesSupport() {
		// Only supported as of version 7.6: https://www.ibm.com/docs/en/i/7.6.0?topic=clause-table-reference
		final var builder = GeneratedValuesSupport.builder( super.getGeneratedValuesSupport() );
		if ( getVersion().isBefore( 7, 6 ) ) {
			builder.disable( GeneratedValuesSupport.Capability.UPDATE_RETURNING );
		}
		return builder.build();
	}

	/**
	 * No support for sequences.
	 */
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return getVersion().isSameOrAfter(7, 3)
				? DB2iSequenceSupport.getInstance()
				: NoSequenceSupport.getInstance();
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion().isSameOrAfter( 7, 3 )
				? SEQUENCE_INFORMATION_EXTRACTOR
				: SequenceInformationExtractors.none();
	}


	@Override
	public LimitHandler getLimitHandler() {
		return getVersion().isSameOrAfter(7, 3)
				? FetchLimitHandler.INSTANCE
				: LegacyDB2LimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion().isSameOrAfter(7, 3)
				? DB2IdentityColumnSupport.INSTANCE
				: DB2zIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new DB2iSqlAstTranslator<>( request, getVersion() );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return RowIdSupports.requestedName( null, ROWID, " rowid not null generated always" );
	}







}
