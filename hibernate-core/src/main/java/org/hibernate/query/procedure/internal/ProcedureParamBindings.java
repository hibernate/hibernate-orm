/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.ParameterMode;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.procedure.ParameterBind;
import org.hibernate.procedure.internal.ParameterBindImpl;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterListBinding;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class ProcedureParamBindings implements QueryParameterBindings {
	private final ProcedureParameterMetadata parameterMetadata;
	private final ProcedureCallImpl procedureCall;

	private final Map<ProcedureParameterImplementor, ParameterBind> bindingMap = new HashMap<>();

	public ProcedureParamBindings(
			ProcedureParameterMetadata parameterMetadata,
			ProcedureCallImpl procedureCall) {
		this.parameterMetadata = parameterMetadata;
		this.procedureCall = procedureCall;
	}

	public ProcedureParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	public ProcedureCallImpl getProcedureCall() {
		return procedureCall;
	}

	@Override
	public boolean isBound(QueryParameter parameter) {
		return getBinding( parameter ).isBound();
	}

	@Override
	public <T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter) {
		final ProcedureParameterImplementor<T> procParam = parameterMetadata.resolve( parameter );
		ParameterBind binding = bindingMap.get( procParam );

		if ( binding == null ) {
			if ( ! parameterMetadata.containsReference( parameter ) ) {
				throw new IllegalArgumentException( "Passed parameter is not registered with this query" );
			}

			binding = new ParameterBindImpl( procParam, this );
			bindingMap.put( procParam, binding );
		}

		return binding;
	}

	@Override
	public <T> QueryParameterBinding<T> getBinding(String name) {
		return getBinding( parameterMetadata.getQueryParameter( name ) );
	}

	@Override
	public <T> QueryParameterBinding<T> getBinding(int position) {
		return getBinding( parameterMetadata.getQueryParameter( position ) );
	}

	@Override
	public void verifyParametersBound(boolean callable) {
		parameterMetadata.visitRegistrations(
				queryParameter -> {
					final ProcedureParameterImplementor procParam = (ProcedureParameterImplementor) queryParameter;
					if ( procParam.getMode() == ParameterMode.IN
							|| procParam.getMode() == ParameterMode.INOUT ) {
						if ( !getBinding( procParam ).isBound() ) {
							// depending on "pass nulls" this might be ok...
							//  for now, just log a warning
						}
					}
				}
		);
	}

	@Override
	public String expandListValuedParameters(String queryString, SharedSessionContractImplementor producer) {
		return queryString;
	}

	@Override
	public <T> QueryParameterListBinding<T> getQueryParameterListBinding(QueryParameter<T> parameter) {
		return null;
	}

	@Override
	public <T> QueryParameterListBinding<T> getQueryParameterListBinding(String name) {
		return null;
	}

	@Override
	public <T> QueryParameterListBinding<T> getQueryParameterListBinding(int position) {
		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// I think these are not needed for proc call execution

	@Override
	public Type[] collectPositionalBindTypes() {
		return new Type[0];
	}

	@Override
	public Object[] collectPositionalBindValues() {
		return new Object[0];
	}

	@Override
	public Map<String, TypedValue> collectNamedParameterBindings() {
		return null;
	}
}
