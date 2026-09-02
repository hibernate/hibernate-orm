/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;


import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.community.dialect.identity.internal.DB2zIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.FetchLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.community.dialect.sequence.CommunitySequenceSupports;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;

import jakarta.persistence.TemporalType;


import static org.hibernate.type.SqlTypes.ROWID;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;

/**
 * An SQL dialect for DB2 for z/OS, previously known as known as Db2 UDB for z/OS and Db2 UDB for z/OS and OS/390.
 *
 * @author Christian Beikov
 */
public class DB2zLegacyDialect extends DB2LegacyDialect implements TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	final static DatabaseVersion DB2_LUW_VERSION9 = DatabaseVersion.make( 9, 0);

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 7 );

	public DB2zLegacyDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
	}

	public DB2zLegacyDialect() {
		this( DEFAULT_VERSION );
	}

	public DB2zLegacyDialect(DatabaseVersion version) {
		super(version);
	}

	@Override
	protected LockingSupport buildLockingSupport() {
		return StandardLockingSupports.db2z();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		if ( getVersion().isSameOrAfter( 12 ) ) {
			CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
			functionFactory.listagg( null );
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
			functionFactory.regexpLike();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		if ( getVersion().isAfter( 10 ) ) {
			switch ( sqlTypeCode ) {
				case TIME_WITH_TIMEZONE:
				case TIMESTAMP_WITH_TIMEZONE:
					// See https://www.ibm.com/support/knowledgecenter/SSEPEK_10.0.0/wnew/src/tpc/db2z_10_timestamptimezone.html
					return "timestamp with time zone";
			}
		}
		return super.columnType( sqlTypeCode );
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION9;
	}

	@Override
	@SPI(IMPLEMENT)
	protected UniqueDelegate createUniqueDelegate() {
		//TODO: when was 'create unique where not null index' really first introduced?
		return getVersion().isSameOrAfter(11)
				//use 'create unique where not null index'
				? UniqueDelegates.nullableIndex( this )
				//ignore unique keys on nullable columns in earlier versions
				: UniqueDelegates.skipNullable( this );
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
	public PredicateSupport getPredicateSupport() {
		// DISTINCT FROM is supported at least since DB2 z/OS 9.0
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.DISTINCT_FROM, true )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return getVersion().isAfter(10) ? TimeZoneSupport.NATIVE : TimeZoneSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return getVersion().isBefore(8)
				? org.hibernate.dialect.sequence.spi.SequenceSupports.none()
				: CommunitySequenceSupports.db2z();
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from sysibm.syssequences" )
					.sequenceNameColumn( "seqname" )
					.withoutCatalog()
					.schemaColumn( "seqschema" )
					.startValueColumn( "start" )
					.minimumValueColumn( "minvalue" )
					.maximumValueColumn( "maxvalue" )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion().isBefore( 8 )
				? SequenceInformationExtractors.none()
				: SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return getVersion().isBefore(12)
				? FetchLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return DB2zIdentityColumnSupport.INSTANCE;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder( super.getSubquerySupport() )
				.feature( SubquerySupport.Feature.LATERAL, true )
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder( super.getCteSupport() )
				.recursiveFeature(
						CteSupport.RecursiveFeature.RECURSIVE,
						getVersion().isSameOrAfter( 11 )
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final StringBuilder pattern = new StringBuilder();
		pattern.append("add_");
		switch (unit) {
			case NATIVE, NANOSECOND -> pattern.append("second");
			//note: DB2 does not have add_weeks()
			case WEEK -> pattern.append("day");
			case QUARTER -> pattern.append("month");
			default -> pattern.append("?1");
		}
		pattern.append("s(");
		final String timestampExpression;
		if ( unit.isDateUnit() ) {
			if ( temporalType == TemporalType.TIME ) {
				timestampExpression = "timestamp('1970-01-01',?3)";
			}
			else {
				timestampExpression = "?3";
			}
		}
		else {
			if ( temporalType == TemporalType.DATE ) {
				timestampExpression = "cast(?3 as timestamp)";
			}
			else {
				timestampExpression = "?3";
			}
		}
		pattern.append(timestampExpression);
		pattern.append(",");
		switch (unit) {
			case NANOSECOND -> pattern.append("(?2)/1e9");
			case WEEK -> pattern.append("(?2)*7");
			case QUARTER -> pattern.append("(?2)*3");
			default -> pattern.append("?2");
		}
		pattern.append(")");
		return pattern.toString();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new DB2zLegacySqlAstTranslator<>( request, getVersion() );
			}
		};
	}

	// I speculate that this is a correct implementation of rowids for DB2 for z/OS,
	// just on the basis of the DB2 docs, but I currently have no way to test it
	// Note that the implementation inherited from DB2Dialect for LUW will not work!

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return RowIdSupports.requestedName(
				"rowid_",
				ROWID,
				" rowid not null generated always"
		);
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		// DB2 z/OS has a VALUES statement, but that doesn't support multiple values
		return ValuesListSupport.INSERT_ONLY;
	}
}
