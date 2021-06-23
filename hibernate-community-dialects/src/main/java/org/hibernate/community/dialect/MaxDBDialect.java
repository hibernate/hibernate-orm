/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.community.dialect.sequence.MaxDBSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorSAPDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;

import java.sql.DatabaseMetaData;
import java.sql.Types;

/**
 * A SQL dialect compatible with SAP MaxDB.
 *
 * @author Brad Clow
 */
public class MaxDBDialect extends Dialect {

	public MaxDBDialect() {
		super();
		registerColumnType( Types.TINYINT, "smallint" );

		registerColumnType( Types.BIGINT, "fixed(19,0)" );

		registerColumnType( Types.NUMERIC, "fixed($p,$s)" );
		registerColumnType( Types.DECIMAL, "fixed($p,$s)" );

		//no explicit precision
		registerColumnType(Types.TIMESTAMP, "timestamp");
		registerColumnType(Types.TIMESTAMP_WITH_TIMEZONE, "timestamp");

		registerColumnType( Types.VARBINARY, "long byte" );

		registerColumnType( Types.CLOB, "long varchar" );
		registerColumnType( Types.BLOB, "long byte" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	}

	@Override
	public JdbcTypeDescriptor resolveSqlTypeDescriptor(
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeDescriptorRegistry jdbcTypeDescriptorRegistry) {
		switch ( jdbcTypeCode ) {
			case Types.NUMERIC:
			case Types.DECIMAL:
				if ( precision == 19 && scale == 0 ) {
					return jdbcTypeDescriptorRegistry.getDescriptor( Types.BIGINT );
				}
		}
		return super.resolveSqlTypeDescriptor( jdbcTypeCode, precision, scale, jdbcTypeDescriptorRegistry );
	}

	@Override
	public int getVersion() {
		return 0;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.initcap( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.daynameMonthname( queryEngine );
		CommonFunctionFactory.dateTimeTimestamp( queryEngine );
		CommonFunctionFactory.ceiling_ceil( queryEngine );
		CommonFunctionFactory.week_weekofyear( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.coalesce_value( queryEngine );
		//since lpad/rpad are not actually useful padding
		//functions, map them to lfill/rfill
		CommonFunctionFactory.pad_fill( queryEngine );
		CommonFunctionFactory.datediff( queryEngine );
		CommonFunctionFactory.adddateSubdateAddtimeSubtime( queryEngine );
		CommonFunctionFactory.addMonths( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerPattern( "extract", "?1(?2)", StandardBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "nullif", "case ?1 when ?2 then null else ?1 end" )
				.setExactArgumentCount(2)
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "index" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 4 )
				.register();

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				StandardBasicTypes.INTEGER, "index(?2, ?1)", "index(?2, ?1, ?3)"
		).setArgumentListSignature("(pattern, string[, start])");
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new MaxDBSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public String trimPattern(TrimSpec specification, char character) {
		return AbstractTransactSQLDialect.replaceLtrimRtrim( specification, character);
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final StringBuilder res = new StringBuilder( 30 )
				.append( " foreign key " )
				.append( constraintName )
				.append( " (" )
				.append( String.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			res.append( " (" )
					.append( String.join( ", ", primaryKey ) )
					.append( ')' );
		}

		return res.toString();
	}

	public String getAddForeignKeyConstraintString(
			String constraintName,
			String foreignKeyDefinition) {
		return foreignKeyDefinition;
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " primary key ";
	}

	@Override
	public String getNullColumnString() {
		return " null";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return MaxDBSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from domain.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorSAPDBDatabaseImpl.INSTANCE;
	}

	@Override
	public String getFromDual() {
		return "from dual";
	}

	@Override
	public boolean supportsSelectQueryWithoutFromClause() {
		return false;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}


	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );

//		return new LocalTemporaryTableBulkIdStrategy(
//				new IdTableSupportStandardImpl() {
//					@Override
//					public String generateIdTableName(String baseName) {
//						return "temp." + super.generateIdTableName( baseName );
//					}
//
//					@Override
//					public String getCreateIdTableStatementOptions() {
//						return "ignore rollback";
//					}
//				},
//				AfterUseAction.DROP,
//				null
//		);
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return false;
	}
}

