/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.DB2SubstringFunction;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.DB2zIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.internal.DB2LockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.FetchLimitHandler;
import org.hibernate.dialect.pagination.LegacyDB2LimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.DB2iSequenceSupport;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import java.util.List;

/**
 * An SQL dialect for DB2 for iSeries previously known as DB2/400.
 *
 * @author Peter DeGregorio (pdegregorio)
 * @author Christian Beikov
 */
public class DB2iLegacyDialect extends DB2LegacyDialect {

	final static DatabaseVersion DB2_LUW_VERSION9 = DatabaseVersion.make( 9, 0);

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 7 );

	public DB2iLegacyDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
		registerKeywords( info );
	}

	public DB2iLegacyDialect() {
		this( DEFAULT_VERSION );
	}

	public DB2iLegacyDialect(DatabaseVersion version) {
		super(version);
	}

	@Override
	protected LockingSupport buildLockingSupport() {
		return DB2LockingSupport.forDB2i();
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		// DB2 for i doesn't allow code units: https://www.ibm.com/docs/en/i/7.1.0?topic=functions-substring
		functionContributions.getFunctionRegistry().register(
				"substring",
				new DB2SubstringFunction( false, functionContributions.getTypeConfiguration() )
		);
		if ( getVersion().isSameOrAfter( 7, 2 ) ) {
			CommonFunctionFactory functionFactory = new CommonFunctionFactory( functionContributions );
			functionFactory.listagg( null );
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		}
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION9;
	}

	@Override
	protected UniqueDelegate createUniqueDelegate() {
		//TODO: when was 'create unique where not null index' really first introduced?
		return getVersion().isSameOrAfter(7, 1)
				//use 'create unique where not null index'
				? new AlterTableUniqueIndexDelegate(this)
				//ignore unique keys on nullable columns in earlier versions
				: new SkipNullableUniqueDelegate(this);
	}

	@Override
	public String getCreateIndexString(boolean unique) {
		// we only create unique indexes, as opposed to unique constraints,
		// when the column is nullable, so safe to infer unique => nullable
		return unique ? "create unique where not null index" : "create index";
	}

	@Override
	public String getCreateIndexTail(boolean unique, List<Column> columns) {
		return "";
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return true;
	}

	/**
	 * No support for sequences.
	 */
	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion().isSameOrAfter(7, 3)
				? DB2iSequenceSupport.INSTANCE : NoSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		if ( getVersion().isSameOrAfter(7,3) ) {
			return "select distinct sequence_name from qsys2.syssequences " +
					"where current_schema='*LIBL' and sequence_schema in (select schema_name from qsys2.library_list_info) " +
					"or sequence_schema=current_schema";
		}
		else {
			return null;
		}
	}

	@Override
	public LimitHandler getLimitHandler() {
		return getVersion().isSameOrAfter(7, 3)
				? FetchLimitHandler.INSTANCE : LegacyDB2LimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion().isSameOrAfter(7, 3)
				? DB2IdentityColumnSupport.INSTANCE
				: DB2zIdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean supportsLateral() {
		return getVersion().isSameOrAfter( 7, 1 );
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return getVersion().isSameOrAfter( 7, 1 );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {

			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DB2iLegacySqlAstTranslator<>( sessionFactory, statement, getVersion() );
			}
		};
	}
}
