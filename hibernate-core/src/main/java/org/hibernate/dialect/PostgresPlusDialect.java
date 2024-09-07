/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation;

import jakarta.persistence.TemporalType;

/**
 * An SQL dialect for Postgres Plus
 *
 * @author Jim Mlodgenski
 */
public class PostgresPlusDialect extends PostgreSQLDialect {

	/**
	 * Constructs a PostgresPlusDialect
	 */
	public PostgresPlusDialect() {
		super();
	}

	public PostgresPlusDialect(DialectResolutionInfo info) {
		super( info );
	}

	public PostgresPlusDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.soundex();
		functionFactory.rownumRowid();
		functionFactory.sysdate();
		functionFactory.systimestamp();

		if ( getVersion().isSameOrAfter( 14 ) ) {
			// Support for these functions were apparently only added in version 14
			functionFactory.bitand();
			functionFactory.bitor();
			functionContributions.getFunctionRegistry().patternDescriptorBuilder(
							"bitxor",
							"(bitor(?1,?2)-bitand(?1,?2))"
					)
					.setExactArgumentCount( 2 )
					.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
					.register();
		}
		else {
			functionContributions.getFunctionRegistry().patternDescriptorBuilder(
							"bitxor",
							"((?1|?2)-(?1&?2))"
					)
					.setExactArgumentCount( 2 )
					.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
					.register();
		}
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.STRING ) {
			switch ( from ) {
				case DATE:
					return "to_char(?1,'YYYY-MM-DD')";
				case TIME:
					return "to_char(?1,'HH24:MI:SS')";
				case TIMESTAMP:
					return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9')";
				case OFFSET_TIMESTAMP:
					return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9TZH:TZM')";
				case ZONE_TIMESTAMP:
					return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9 TZR')";
			}
		}
		return super.castPattern( from, to );
	}

	@Override
	public String currentTimestamp() {
		return "current_timestamp";
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( toTemporalType == TemporalType.DATE && fromTemporalType == TemporalType.DATE ) {
			// special case: subtraction of two dates results in an INTERVAL on Postgres Plus
			// because there is no date type i.e. without time for Oracle compatibility
			return super.timestampdiffPattern( unit, TemporalType.TIMESTAMP, TemporalType.TIMESTAMP );
		}
		return super.timestampdiffPattern( unit, fromTemporalType, toTemporalType );
	}

	@Override
	public boolean isEmptyStringTreatedAsNull() {
		return true;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter( col, Types.REF );
		col++;
		return col;
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid_generate_v1";
	}

	@Override
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		// Postgres Plus does not support full merge semantics -
		// https://www.enterprisedb.com/docs/migrating/oracle/oracle_epas_comparison/notable_differences/
		return new OptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new PostgreSQLSqlAstTranslator<>( sessionFactory, statement ) {
					@Override
					public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
						if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
							appendSql( "floor" );
						}
						super.visitBinaryArithmeticExpression(arithmeticExpression);
					}
				};
			}
		};
	}
}
