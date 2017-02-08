/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
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

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "exp", new StandardSQLFunction( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "power", new StandardSQLFunction( "power" ) );
		registerFunction( "acos", new StandardSQLFunction( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cosh", new StandardSQLFunction( "cosh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sinh", new StandardSQLFunction( "sinh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tanh", new StandardSQLFunction( "tanh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "radians", new StandardSQLFunction( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new StandardSQLFunction( "atan2", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "round", new StandardSQLFunction( "round" ) );
		registerFunction( "trunc", new StandardSQLFunction( "trunc" ) );
		registerFunction( "ceil", new StandardSQLFunction( "ceil" ) );
		registerFunction( "floor", new StandardSQLFunction( "floor" ) );
		registerFunction( "greatest", new StandardSQLFunction( "greatest" ) );
		registerFunction( "least", new StandardSQLFunction( "least" ) );

		registerFunction( "time", new StandardSQLFunction( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "timestamp", new StandardSQLFunction( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "date", new StandardSQLFunction( "date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "microsecond", new StandardSQLFunction( "microsecond", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "second", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "second(?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "minute(?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "hour(?1)" ) );
		registerFunction( "day", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "day(?1)" ) );
		registerFunction( "month", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "month(?1)" ) );
		registerFunction( "year", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "year(?1)" ) );

		registerFunction( "extract", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "?1(?3)" ) );

		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "weekofyear", new StandardSQLFunction( "weekofyear", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "replace", new StandardSQLFunction( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSQLFunction( "translate", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substr", new StandardSQLFunction( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "initcap", new StandardSQLFunction( "initcap", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lower", new StandardSQLFunction( "lower", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lfill", new StandardSQLFunction( "ltrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rfill", new StandardSQLFunction( "rtrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "upper", new StandardSQLFunction( "upper", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardSpiBasicTypes.STRING ) );
		registerFunction( "index", new StandardSQLFunction( "index", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "value", new StandardSQLFunction( "value" ) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "substring", new StandardSQLFunction( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "locate", new StandardSQLFunction( "index", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new StandardSQLFunction( "value" ) );

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
