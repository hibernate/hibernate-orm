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
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
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
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.registerNamed( "abs" );
		registry.registerNamed( "sign", StandardSpiBasicTypes.INTEGER );

		registry.registerNamed( "exp", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "ln", StandardSpiBasicTypes.DOUBLE );
		registry.namedTemplateBuilder( "log", "ln" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		registry.registerNoArgs( "pi", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "power" );
		registry.registerNamed( "acos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "asin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "atan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "cos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "cosh", StandardSpiBasicTypes.DOUBLE );
		registry.namedTemplateBuilder( "cot", "cos" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		registry.registerNamed( "sin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sinh", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "tan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "tanh", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "radians", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "degrees", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "atan2", StandardSpiBasicTypes.DOUBLE );

		registry.registerNamed( "round" );
		registry.registerNamed( "trunc" );
		registry.registerNamed( "ceil" );
		registry.registerNamed( "floor" );
		registry.registerNamed( "greatest" );
		registry.registerNamed( "least" );

		registry.registerNamed( "time", StandardSpiBasicTypes.TIME );
		registry.registerNamed( "timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "date", StandardSpiBasicTypes.DATE );
		registry.registerNamed( "microsecond", StandardSpiBasicTypes.INTEGER );

		registry.registerPattern( "second", "second(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "minute", "minute(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "hour", "hour(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "day", "day(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "month", "month(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "year", "year(?1)", StandardSpiBasicTypes.INTEGER );

		registry.registerPattern( "extract", "?1(?3)", StandardSpiBasicTypes.INTEGER );

		registry.registerNamed( "dayname", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "monthname", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "dayofmonth", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "dayofweek", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "dayofyear", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "weekofyear", StandardSpiBasicTypes.INTEGER );

		registry.registerNamed( "replace", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "translate", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "lpad", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "rpad", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "substr", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "initcap", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "lower", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "ltrim", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "rtrim", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "lfill", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "rfill", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "soundex", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "upper", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "ascii", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "index", StandardSpiBasicTypes.INTEGER );

		registry.registerNamed( "value" );

		registry.registerVarArgs( "concat", StandardSpiBasicTypes.STRING, "(", "||", ")" );
		registry.registerAlternateKey( "substring", "substr" );
		registry.registerAlternateKey( "locate", "index" );
		registry.registerAlternateKey( "coalesce", "value" );
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
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() ) {
			@Override
			protected String determineIdTableName(String baseName) {
				return "temp." + super.determineIdTableName( baseName );
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
