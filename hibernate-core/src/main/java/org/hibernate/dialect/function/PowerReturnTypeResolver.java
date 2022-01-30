/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;
import java.util.function.Supplier;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.extractArgumentJdbcMapping;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.extractArgumentValuedMapping;

/**
 * @author Christian Beikov
 */
class PowerReturnTypeResolver implements FunctionReturnTypeResolver {

	private final BasicType<Double> doubleType;

	PowerReturnTypeResolver(TypeConfiguration typeConfiguration) {
		this.doubleType = typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.DOUBLE);
	}

	@Override
	public ReturnableType<?> resolveFunctionReturnType(
			ReturnableType<?> impliedType,
			List<? extends SqmTypedNode<?>> arguments,
			TypeConfiguration typeConfiguration) {
		final JdbcMapping baseType = extractArgumentJdbcMapping( typeConfiguration, arguments, 1 );
		final JdbcMapping powerType = extractArgumentJdbcMapping( typeConfiguration, arguments, 2 );

		if ( baseType.getJdbcType().isDecimal() ) {
			return (ReturnableType<?>) arguments.get(0).getNodeType();
		}
		else if (powerType.getJdbcType().isDecimal()) {
			return (ReturnableType<?>) arguments.get(1).getNodeType();
		}
		return typeConfiguration.getBasicTypeForJavaType(Double.class);
	}

	@Override
	public BasicValuedMapping resolveFunctionReturnType(
			Supplier<BasicValuedMapping> impliedTypeAccess, List<? extends SqlAstNode> arguments) {
		final BasicValuedMapping baseMapping = extractArgumentValuedMapping( arguments,1 );
		final BasicValuedMapping powerMapping = extractArgumentValuedMapping( arguments, 2 );
		if ( baseMapping.getJdbcMapping().getJdbcType().isDecimal() ) {
			return baseMapping;
		}
		else if ( powerMapping.getJdbcMapping().getJdbcType().isDecimal() ) {
			return powerMapping;
		}
		return doubleType;
	}
}
