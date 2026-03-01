/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.procedure;

import java.util.List;
import java.util.function.IntFunction;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Abstract base for {@link StoredProcedureSupport} implementations.
 *
 * @author Gavin King
 */
@Incubating
public abstract class AbstractStoredProcedureSupport implements StoredProcedureSupport {

	@Override
	public boolean supportsStoredProcedures() {
		return true;
	}

	static String typeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect) {
		final int typeCode = jdbcMapping.getJdbcType().getDdlTypeCode();
		return jdbcMapping instanceof Type type
				? registry.getTypeName( typeCode, Size.nil(), type )
				: registry.getTypeName( typeCode, dialect );
	}

	@Override
	public String parameterTypeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect) {
		return typeName( jdbcMapping, registry, dialect );
	}

	@Override
	public String resultTypeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect) {
		return typeName( jdbcMapping, registry, dialect );
	}

	@Override
	public String mutationInvocationSql(String name, int parameterCount) {
		return "{call " + name + renderCallParameters( parameterCount ) + "}";
	}

	@Override
	public String selectInvocationSql(String name, int parameterCount) {
		return mutationInvocationSql( name, parameterCount );
	}

	@Override
	public boolean isSelectCallable() {
		return true;
	}

	@Override
	public String createSelectProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes,
			List<String> resultTypeNames,
			List<String> resultColumnNames) {
		return createMutationProcedureDdl( name, statement, parameterTypes );
	}

	@Override
	public String dropSelectProcedureDdl(String name, List<String> parameterTypes) {
		return dropMutationProcedureDdl( name );
	}

	@Override
	public boolean requiresSelectResultDescriptor() {
		return false;
	}

	static String renderParameterDeclarations(
			List<String> parameterTypes,
			String parameterPrefix,
			String delimiter) {
		return renderCommaSeparated( parameterTypes.size(),
				index -> parameterPrefix + index + delimiter + parameterTypes.get( index - 1 ) );
	}

	static String renderCommaSeparated(
			int parameterCount,
			IntFunction<String> renderer) {
		final var commas = new StringBuilder();
		for ( int i = 1; i <= parameterCount; i++ ) {
			if ( i > 1 ) {
				commas.append( ", " );
			}
			commas.append( renderer.apply( i ) );
		}
		return commas.toString();
	}

	static String renderCallParameters(int parameterCount) {
		final var result = new StringBuilder( "(" );
		for ( int i = 0; i < parameterCount; i++ ) {
			if ( i > 0 ) {
				result.append( ',' );
			}
			result.append( '?' );
		}
		return result.append( ')' ).toString();
	}

	static String replaceJdbcParameters(
			String sql,
			IntFunction<String> replacement) {
		final var result = new StringBuilder( sql.length() + 32 );
		final class Recognizer implements ParameterRecognizer {
			int index = 0;
			@Override
			public void ordinalParameter(int sourcePosition) {
				result.append( replacement.apply( ++index ) );
			}

			@Override
			public void namedParameter(String name, int sourcePosition) {
				result.append( ':' ).append( name );
			}

			@Override
			public void jpaPositionalParameter(int label, int sourcePosition) {
				result.append( '?' ).append( label );
			}

			@Override
			public void other(char character) {
				result.append( character );
			}
		}
		ParameterParser.parse( sql, new Recognizer() );
		return result.toString();
	}
}
