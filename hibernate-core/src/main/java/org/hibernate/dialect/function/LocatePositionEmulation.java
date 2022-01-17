/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.mapping.Collection;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * @author Gavin King
 */
public class LocatePositionEmulation extends AbstractSqmFunctionDescriptor {

	public LocatePositionEmulation(TypeConfiguration typeConfiguration) {
		super(
				"position",
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), STRING, STRING ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		if ( arguments instanceof ArrayList ) {
			Collections.swap( arguments, 0, 1 );
		}
		else {
			List<SqmTypedNode<?>> argumentTemp = new ArrayList<>( arguments.size() );
			argumentTemp.add( arguments.get( 1 ) );
			argumentTemp.add( arguments.get( 0 ) );
			for ( int i = 2; i < arguments.size(); i++ ) {
				argumentTemp.add( arguments.get( i ) );
			}
			arguments = argumentTemp;
		}
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "locate" )
				.generateSqmExpression( arguments, impliedResultType, queryEngine, typeConfiguration );
	}

	@Override
	public String getArgumentListSignature() {
		return "(STRING pattern in STRING string)";
	}
}
