/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.DB2zIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.FetchLimitHandler;
import org.hibernate.dialect.pagination.LegacyDB2LimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.DB2iSequenceSupport;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import java.util.List;

import static org.hibernate.type.SqlTypes.ROWID;

/**
 * A SQL dialect for DB2 for IBM i version 7.1 and above, previously known as "DB2/400".
 *
 * @author Peter DeGregorio (pdegregorio)
 * @author Christian Beikov
 */
public class DB2iDialect extends DB2Dialect {

	private final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 7, 1 );
	final static DatabaseVersion DB2_LUW_VERSION = DB2Dialect.MINIMUM_VERSION;

	public DB2iDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( MINIMUM_VERSION ) );
		registerKeywords( info );
	}

	public DB2iDialect() {
		this( MINIMUM_VERSION );
	}

	public DB2iDialect(DatabaseVersion version) {
		super(version);
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		if ( getVersion().isSameOrAfter( 7, 2 ) ) {
			CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
			functionFactory.listagg( null );
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		}
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION;
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
	public boolean supportsIfExistsBeforeTableName() {
		return false;
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
	public boolean supportsSkipLocked() {
		return true;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DB2iSqlAstTranslator<>( sessionFactory, statement, getVersion() );
			}
		};
	}

	// I speculate that this is a correct implementation of rowids for DB2 for i,
	// just on the basis of the DB2 docs, but I currently have no way to test it
	// Note that the implementation inherited from DB2Dialect for LUW will not work!

	@Override
	public String rowId(String rowId) {
		return rowId == null || rowId.isEmpty() ? "rowid_" : rowId;
	}

	@Override
	public int rowIdSqlType() {
		return ROWID;
	}

	@Override
	public String getRowIdColumnString(String rowId) {
		return rowId( rowId ) + " rowid not null generated always";
	}
}
