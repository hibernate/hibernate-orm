/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.exec.spi.JdbcOperation;

import jakarta.persistence.TemporalType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.dialect.type.spi.StringValueSemantics;

/**
 * An SQL dialect for Postgres Plus
 *
 * @author Jim Mlodgenski
 */
public class PostgresPlusLegacyDialect extends PostgreSQLLegacyDialect implements CurrentTemporalSupport, TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private static final RefCursorSupportFactory REF_CURSOR_SUPPORT_FACTORY = RefCursorSupports.postgresPlus();

	/**
	 * Constructs a PostgresPlusDialect
	 */
	public PostgresPlusLegacyDialect() {
		super();
	}

	public PostgresPlusLegacyDialect(DialectResolutionInfo info) {
		super( info );
	}

	public PostgresPlusLegacyDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.arrayGet_bracket( false );
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "current_timestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( toTemporalType == TemporalType.DATE && fromTemporalType == TemporalType.DATE ) {
			// special case: subtraction of two dates results in an INTERVAL on Postgres Plus
			// because there is no date type i.e. without time for Oracle compatibility
			return super.timestampdiffPattern( unit, TemporalType.TIMESTAMP, TemporalType.TIMESTAMP );
		}
		return super.timestampdiffPattern( unit, fromTemporalType, toTemporalType );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public StringValueSemantics getStringValueSemantics() {
		return StringValueSemantics.EMPTY_STRING_AS_NULL;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RefCursorSupportFactory getRefCursorSupportFactory() {
		return REF_CURSOR_SUPPORT_FACTORY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new PostgreSQLLegacySqlAstTranslator<>( request ) {
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
