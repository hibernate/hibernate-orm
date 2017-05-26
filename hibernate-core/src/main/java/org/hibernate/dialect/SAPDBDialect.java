/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgsSqmFunctionTemplate;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.StandardSqmFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.type.spi.StandardSpiBasicTypes;

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

		registerFunction( "abs", new StandardSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new StandardSqmFunctionTemplate( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "exp", new StandardSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new StandardSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgsSqmFunctionTemplate( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "power", new StandardSqmFunctionTemplate( "power" ) );
		registerFunction( "acos", new StandardSqmFunctionTemplate( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSqmFunctionTemplate( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cosh", new StandardSqmFunctionTemplate( "cosh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sinh", new StandardSqmFunctionTemplate( "sinh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSqmFunctionTemplate( "tan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tanh", new StandardSqmFunctionTemplate( "tanh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "radians", new StandardSqmFunctionTemplate( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSqmFunctionTemplate( "degrees", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new StandardSqmFunctionTemplate( "atan2", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "round", new StandardSqmFunctionTemplate( "round" ) );
		registerFunction( "trunc", new StandardSqmFunctionTemplate( "trunc" ) );
		registerFunction( "ceil", new StandardSqmFunctionTemplate( "ceil" ) );
		registerFunction( "floor", new StandardSqmFunctionTemplate( "floor" ) );
		registerFunction( "greatest", new StandardSqmFunctionTemplate( "greatest" ) );
		registerFunction( "least", new StandardSqmFunctionTemplate( "least" ) );

		registerFunction( "time", new StandardSqmFunctionTemplate( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "timestamp", new StandardSqmFunctionTemplate( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "date", new StandardSqmFunctionTemplate( "date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "microsecond", new StandardSqmFunctionTemplate( "microsecond", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "second", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "second(?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "minute(?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "hour(?1)" ) );
		registerFunction( "day", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "day(?1)" ) );
		registerFunction( "month", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "month(?1)" ) );
		registerFunction( "year", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "year(?1)" ) );

		registerFunction( "extract", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "?1(?3)" ) );

		registerFunction( "dayname", new StandardSqmFunctionTemplate( "dayname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "monthname", new StandardSqmFunctionTemplate( "monthname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardSqmFunctionTemplate( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSqmFunctionTemplate( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSqmFunctionTemplate( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "weekofyear", new StandardSqmFunctionTemplate( "weekofyear", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "replace", new StandardSqmFunctionTemplate( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSqmFunctionTemplate( "translate", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lpad", new StandardSqmFunctionTemplate( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSqmFunctionTemplate( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substr", new StandardSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "initcap", new StandardSqmFunctionTemplate( "initcap", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lower", new StandardSqmFunctionTemplate( "lower", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSqmFunctionTemplate( "ltrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rtrim", new StandardSqmFunctionTemplate( "rtrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lfill", new StandardSqmFunctionTemplate( "ltrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rfill", new StandardSqmFunctionTemplate( "rtrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "soundex", new StandardSqmFunctionTemplate( "soundex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "upper", new StandardSqmFunctionTemplate( "upper", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ascii", new StandardSqmFunctionTemplate( "ascii", StandardSpiBasicTypes.STRING ) );
		registerFunction( "index", new StandardSqmFunctionTemplate( "index", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "value", new StandardSqmFunctionTemplate( "value" ) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "substring", new StandardSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "locate", new StandardSqmFunctionTemplate( "index", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new StandardSqmFunctionTemplate( "value" ) );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

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
				.append( StringHelper.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			res.append( " (" )
					.append( StringHelper.join( ", ", primaryKey ) )
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
		return "select sequence_name from domain.sequences";
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
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new LocalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String generateIdTableName(String baseName) {
						return "temp." + super.generateIdTableName( baseName );
					}

					@Override
					public String getCreateIdTableStatementOptions() {
						return "ignore rollback";
					}
				},
				AfterUseAction.DROP,
				null
		);
	}
}
