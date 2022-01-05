/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.QueryException;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

import java.lang.reflect.Type;
import java.util.List;

import static org.hibernate.type.SqlTypes.*;

/**
 * Typechecks the arguments of HQL functions based on the assigned JDBC types.
 */
public class ArgumentTypesValidator implements ArgumentsValidator {
	final ArgumentsValidator delegate;
	private final ParameterType[] types;

	public ArgumentTypesValidator(ArgumentsValidator delegate, ParameterType... types) {
		this.types = types;
		if (types.length==0) {
			System.out.println();
		}
		if (delegate == null ) {
			delegate = StandardArgumentsValidators.exactly(types.length);
		}
		this.delegate = delegate;
	}

	@Override
	public void validate(List<? extends SqmTypedNode<?>> arguments, String functionName) {
		delegate.validate(arguments, functionName);
	}

	@Override
	public void validateSqlTypes(List<? extends SqlAstNode> arguments, String functionName) {
		int count = 0;
		for (int i = 0; i < arguments.size(); i++ ) {
			SqlAstNode argument = arguments.get(i);
			if (argument instanceof Expression) {
				JdbcMappingContainer expressionType = ((Expression) argument).getExpressionType();
				if ( expressionType != null ) {
					ParameterDetector detector = new ParameterDetector();
					argument.accept(detector);
					if ( detector.detected ) {
						count += expressionType.getJdbcTypeCount();
					}
					else {
						count = validateArgument(count, expressionType, functionName);
					}
				}
			}
		}
	}

	private int validateArgument(int count, JdbcMappingContainer expressionType, String functionName) {
		List<JdbcMapping> mappings = expressionType.getJdbcMappings();
		for (JdbcMapping mapping : mappings) {
			ParameterType type = count < types.length ? types[count++] : types[types.length - 1];
			if (type != null) {
				int code = mapping.getJdbcTypeDescriptor().getJdbcTypeCode();
				Type javaType = mapping.getJavaTypeDescriptor().getJavaType();
				switch (type) {
					case STRING:
						if (!isCharacterType(code)) {
							throwError(type, javaType, functionName, count);
						}
						break;
					case NUMERIC:
						if (!isNumericType(code)) {
							throwError(type, javaType, functionName, count);
						}
						break;
					case INTEGER:
						if (!isIntegral(code)) {
							throwError(type, javaType, functionName, count);
						}
						break;
					case BOOLEAN:
						if (code != BOOLEAN && code != BIT) {
							throwError(type, javaType, functionName, count);
						}
					case TEMPORAL:
						if (!isTemporalType(code)) {
							throwError(type, javaType, functionName, count);
						}
						break;
					case DATE:
						if (!hasDatePart(code)) {
							throwError(type, javaType, functionName, count);
						}
						break;
					case TIME:
						if (!hasTimePart(code)) {
							throwError(type, javaType, functionName, count);
						}
						break;
				}
			}
		}
		return count;
	}

	private void throwError(ParameterType type, Type javaType, String functionName, int count) {
		throw new QueryException(
				String.format(
						"Parameter %d of function %s() has type %s, but argument is of type %s",
						count,
						functionName,
						type,
						javaType.getTypeName()
				)
		);
	}

	private static class ParameterDetector extends AbstractSqlAstWalker {
		private boolean detected;
		@Override
		public void visitParameter(JdbcParameter jdbcParameter) {
			detected = true;
		}
	}
}
