/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.procedure.ParameterBind;
import org.hibernate.query.internal.BindingTypeHelper;
import org.hibernate.query.procedure.internal.ProcedureParamBindings;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Implementation of the {@link ParameterBind} contract.
 *
 * @author Steve Ebersole
 */
public class ParameterBindImpl<T> implements ParameterBind<T> {
	private static final Logger log = Logger.getLogger( ParameterBindImpl.class );

	private final ProcedureParameterImplementor procedureParameter;
	private final ProcedureParamBindings procedureParamBindings;

	private boolean isBound;

	private T value;
	private Type hibernateType;

	private TemporalType explicitTemporalType;

	public ParameterBindImpl(
			ProcedureParameterImplementor procedureParameter,
			ProcedureParamBindings procedureParamBindings) {
		this.procedureParameter = procedureParameter;
		this.procedureParamBindings = procedureParamBindings;

		this.hibernateType = procedureParameter.getHibernateType();
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public TemporalType getExplicitTemporalType() {
		return explicitTemporalType;
	}

	@Override
	public boolean isBound() {
		return isBound;
	}

	@Override
	public void setBindValue(T value) {
		internalSetValue( value );

		if ( value != null && hibernateType == null ) {
			hibernateType = procedureParamBindings.getProcedureCall()
					.getSession()
					.getFactory()
					.getTypeResolver()
					.heuristicType( value.getClass().getName() );
			log.debugf( "Using heuristic type [%s] based on bind value [%s] as `bindType`", hibernateType, value );
		}
	}

	private void internalSetValue(T value) {
		if ( procedureParameter.getMode() != ParameterMode.IN && procedureParameter.getMode() != ParameterMode.INOUT ) {
			throw new IllegalStateException( "Can only bind values for IN/INOUT parameters : " + procedureParameter );
		}

		if ( procedureParameter.getParameterType() != null ) {
			if ( value == null ) {
				if ( !procedureParameter.isPassNullsEnabled() ) {
					throw new IllegalArgumentException( "The parameter " +
							( procedureParameter.getName() != null
									? "named [" + procedureParameter.getName() + "]"
									: "at position [" + procedureParameter.getPosition() + "]" )
							+ " was null. You need to call ParameterRegistration#enablePassingNulls(true) in order to pass null parameters." );
				}
			}
			else if ( !procedureParameter.getParameterType().isInstance( value ) &&
					!procedureParameter.getHibernateType().getReturnedClass().isInstance( value ) ) {
				throw new IllegalArgumentException( "Bind value [" + value + "] was not of specified type [" + procedureParameter
						.getParameterType() );
			}
		}

		this.value = value;
		this.isBound = true;
	}

	@Override
	public void setBindValue(T value, Type clarifiedType) {
		internalSetValue( value );
		this.hibernateType = clarifiedType;
		log.debugf( "Using explicit type [%s] as `bindType`", hibernateType, value );
	}

	@Override
	public void setBindValue(T value, TemporalType clarifiedTemporalType) {
		internalSetValue( value );
		this.hibernateType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, hibernateType, value );
		this.explicitTemporalType = clarifiedTemporalType;
		log.debugf( "Using type [%s] (based on TemporalType [%s] as `bindType`", hibernateType, clarifiedTemporalType );
	}

	@Override
	public T getBindValue() {
		if ( !isBound ) {
			throw new IllegalStateException( "Value not yet bound" );
		}
		return value;
	}

	@Override
	public Type getBindType() {
		return hibernateType;
	}
}
