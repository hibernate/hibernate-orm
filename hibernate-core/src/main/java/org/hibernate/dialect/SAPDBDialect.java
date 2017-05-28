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
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
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

		registerFunction( "abs", new NamedSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new NamedSqmFunctionTemplate( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "exp", new NamedSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new NamedSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new NamedSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgsSqmFunctionTemplate( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "power", new NamedSqmFunctionTemplate( "power" ) );
		registerFunction( "acos", new NamedSqmFunctionTemplate( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new NamedSqmFunctionTemplate( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new NamedSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new NamedSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cosh", new NamedSqmFunctionTemplate( "cosh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new NamedSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new NamedSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sinh", new NamedSqmFunctionTemplate( "sinh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new NamedSqmFunctionTemplate( "tan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tanh", new NamedSqmFunctionTemplate( "tanh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "radians", new NamedSqmFunctionTemplate( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new NamedSqmFunctionTemplate( "degrees", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new NamedSqmFunctionTemplate( "atan2", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "round", new NamedSqmFunctionTemplate( "round" ) );
		registerFunction( "trunc", new NamedSqmFunctionTemplate( "trunc" ) );
		registerFunction( "ceil", new NamedSqmFunctionTemplate( "ceil" ) );
		registerFunction( "floor", new NamedSqmFunctionTemplate( "floor" ) );
		registerFunction( "greatest", new NamedSqmFunctionTemplate( "greatest" ) );
		registerFunction( "least", new NamedSqmFunctionTemplate( "least" ) );

		registerFunction( "time", new NamedSqmFunctionTemplate( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "timestamp", new NamedSqmFunctionTemplate( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "date", new NamedSqmFunctionTemplate( "date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "microsecond", new NamedSqmFunctionTemplate( "microsecond", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "second", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "second(?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "minute(?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "hour(?1)" ) );
		registerFunction( "day", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "day(?1)" ) );
		registerFunction( "month", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "month(?1)" ) );
		registerFunction( "year", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "year(?1)" ) );

		registerFunction( "extract", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "?1(?3)" ) );

		registerFunction( "dayname", new NamedSqmFunctionTemplate( "dayname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "monthname", new NamedSqmFunctionTemplate( "monthname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new NamedSqmFunctionTemplate( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new NamedSqmFunctionTemplate( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new NamedSqmFunctionTemplate( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "weekofyear", new NamedSqmFunctionTemplate( "weekofyear", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "replace", new NamedSqmFunctionTemplate( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new NamedSqmFunctionTemplate( "translate", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lpad", new NamedSqmFunctionTemplate( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new NamedSqmFunctionTemplate( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substr", new NamedSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "initcap", new NamedSqmFunctionTemplate( "initcap", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lower", new NamedSqmFunctionTemplate( "lower", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ltrim", new NamedSqmFunctionTemplate( "ltrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rtrim", new NamedSqmFunctionTemplate( "rtrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lfill", new NamedSqmFunctionTemplate( "ltrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rfill", new NamedSqmFunctionTemplate( "rtrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "soundex", new NamedSqmFunctionTemplate( "soundex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "upper", new NamedSqmFunctionTemplate( "upper", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ascii", new NamedSqmFunctionTemplate( "ascii", StandardSpiBasicTypes.STRING ) );
		registerFunction( "index", new NamedSqmFunctionTemplate( "index", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "value", new NamedSqmFunctionTemplate( "value" ) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "substring", new NamedSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "locate", new NamedSqmFunctionTemplate( "index", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new NamedSqmFunctionTemplate( "value" ) );

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
