/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmJsonNullBehavior;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.query.sqm.produce.function.ArgumentTypesValidator.checkArgumentType;
import static org.hibernate.query.sqm.produce.function.ArgumentTypesValidator.isUnknownExpressionType;
import static org.hibernate.type.descriptor.java.JavaTypeHelper.isUnknown;

public class JsonObjectArgumentsValidator implements ArgumentsValidator {

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		if ( !arguments.isEmpty() ) {
			final SqmTypedNode<?> lastArgument = arguments.get( arguments.size() - 1 );
			final int argumentsCount;
			if ( lastArgument instanceof SqmJsonNullBehavior ) {
				argumentsCount = arguments.size() - 1;
			}
			else {
				argumentsCount = arguments.size();
			}
			checkArgumentsCount( argumentsCount );
			for ( int i = 0; i < argumentsCount; i += 2 ) {
				final SqmTypedNode<?> key = arguments.get( i );
				final SqmExpressible<?> nodeType = key.getNodeType();
				final JavaType<?> javaType = nodeType == null
						? null
						: nodeType.getRelationalJavaType();
				if ( !isUnknown( javaType ) ) {
					final DomainType<?> domainType = key.getExpressible().getSqmType();
					if ( domainType instanceof JdbcMapping jdbcMapping ) {
						checkArgumentType(
								i,
								functionName,
								FunctionParameterType.STRING,
								jdbcMapping.getJdbcType(),
								javaType.getJavaTypeClass()
						);
					}
				}
			}
		}
	}

	@Override
	public void validateSqlTypes(List<? extends SqlAstNode> arguments, String functionName) {
		if ( !arguments.isEmpty() ) {
			final SqlAstNode lastArgument = arguments.get( arguments.size() - 1 );
			final int argumentsCount;
			if ( lastArgument instanceof JsonNullBehavior ) {
				argumentsCount = arguments.size() - 1;
			}
			else {
				argumentsCount = arguments.size();
			}
			checkArgumentsCount( argumentsCount );
			for ( int i = 0; i < argumentsCount; i += 2 ) {
				final SqlAstNode argument = arguments.get( i );
				if ( argument instanceof Expression expression ) {
					final JdbcMappingContainer expressionType = expression.getExpressionType();
					if ( expressionType != null && !isUnknownExpressionType( expressionType ) ) {
						final JdbcMapping mapping = expressionType.getSingleJdbcMapping();
						checkArgumentType(
								i,
								functionName,
								FunctionParameterType.STRING,
								mapping.getJdbcType(),
								mapping.getJavaTypeDescriptor().getJavaType()
						);
					}
				}
			}
		}
	}

	private void checkArgumentsCount(int size) {
		if ( ( size & 1 ) == 1 ) {
			throw new FunctionArgumentException(
					String.format(
							"json_object must have an even number of arguments, but found %d",
							size
					)
			);
		}
	}
}
