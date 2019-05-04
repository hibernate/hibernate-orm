/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.naming.Identifier;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorSAPDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * An SQL dialect compatible with SAP DB.
 *
 * @author Brad Clow
 */
public class SAPDBDialect extends Dialect {
	/**
	 * Constructs a SAPDBDialect
	 */
	public SAPDBDialect() {
		super();
		registerColumnType( Types.BIT, "boolean" );
		registerColumnType( Types.BIGINT, "fixed(19,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "fixed(3,0)" );
		registerColumnType( Types.INTEGER, "int" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "long byte" );
		registerColumnType( Types.NUMERIC, "fixed($p,$s)" );
		registerColumnType( Types.CLOB, "long varchar" );
		registerColumnType( Types.BLOB, "long byte" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerNamed( "sign", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerNamed( "exp", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ln", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "log", "ln" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "pi", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "power" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "acos", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "asin", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "atan", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "cos", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "cosh", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "cot", "cos" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		queryEngine.getSqmFunctionRegistry().registerNamed( "sin", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "sinh", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "tan", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "tanh", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "radians", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "degrees", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "atan2", StandardSpiBasicTypes.DOUBLE );

		queryEngine.getSqmFunctionRegistry().registerNamed( "round" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "trunc" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ceil" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "floor" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "greatest" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "least" );

		queryEngine.getSqmFunctionRegistry().registerNamed( "time", StandardSpiBasicTypes.TIME );
		queryEngine.getSqmFunctionRegistry().registerNamed( "timestamp", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNamed( "date", StandardSpiBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "microsecond", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerPattern( "second", "second(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "minute", "minute(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "hour", "hour(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "day", "day(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "month", "month(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "year", "year(?1)", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerPattern( "extract", "?1(?3)", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerNamed( "dayname", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "monthname", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofmonth", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofweek", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofyear", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "weekofyear", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerNamed( "translate", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "lpad", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "rpad", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "substr", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "initcap", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ltrim", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "rtrim", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "lfill", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "rfill", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "soundex", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ascii", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "index", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerNamed( "value" );

		queryEngine.getSqmFunctionRegistry().registerVarArgs( "concat", StandardSpiBasicTypes.STRING, "(", "||", ")" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "substring", "substr" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "locate", "index" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "coalesce", "value" );
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
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from dual";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
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
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				return new Identifier( "temp." + super.determineIdTableName( baseName ).getText(), false );
			}
		};
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new LocalTempTableExporter() {
			@Override
			protected String getCreateOptions() {
				return "ignore rollback";
			}
		};
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return false;
	}
}
