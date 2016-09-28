/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.procedure.ParameterBind;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.type.mapper.spi.Type;

/**
 * @author Steve Ebersole
 */
public class ParameterRegistrationStandardImpl<T> implements ParameterRegistrationImplementor<T> {
	private final ParameterManager parameterManager;

	private final QueryParameterImplementor<T> parameter;
	private final ParameterBindImpl<T> binding;

	private boolean passNulls;
	private Type hibernateType;

	public ParameterRegistrationStandardImpl(
			ParameterManager parameterManager,
			QueryParameterImplementor<T> parameter) {
		this.parameterManager = parameterManager;
		this.parameter = parameter;

		if ( parameter.getMode() == ParameterMode.IN || parameter.getMode() == ParameterMode.INOUT ) {
			binding = parameterManager.makeBinding( parameter );
			this.hibernateType = parameter.getHibernateType();
		}
		else {
			binding = null;
		}
	}

	@Override
	public ProcedureCallImplementor getProcedureCall() {
		return parameterManager.getProcedureCall();
	}


	@Override
	public void prepare(CallableStatement statement, int i) throws SQLException {

	}

	@Override
	public int[] getSqlTypes() {
		return new int[0];
	}

	@Override
	public T extract(CallableStatement statement) {
		return null;
	}

	@Override
	public boolean isPassNullsEnabled() {
		return passNulls;
	}

	@Override
	public void enablePassingNulls(boolean enabled) {
		this.passNulls = enabled;
	}

	@Override
	public ParameterBind<T> getBind() {
		return binding;
	}

	@Override
	public void bindValue(T value) {
		validateBindability();
		if ( hibernateType != null ) {
			binding.setBindValue( value, hibernateType );
		}
		else {
			binding.setBindValue( value );
		}
	}

	private void validateBindability() {
		if ( ! canBind() ) {
			throw new ParameterMisuseException( "Cannot bind value to non-input parameter : " + this );
		}
	}

	private boolean canBind() {
		return getMode() == ParameterMode.IN || getMode() == ParameterMode.INOUT;
	}

	@Override
	public void bindValue(T value, TemporalType explicitTemporalType) {
		validateBindability();
		binding.setBindValue( value, explicitTemporalType );
	}

	@Override
	public String getName() {
		return parameter.getName();
	}

	@Override
	public Integer getPosition() {
		return parameter.getPosition();
	}

	@Override
	public Class<T> getParameterType() {
		return parameter.getParameterType();
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return false;
	}

	@Override
	public Type getHibernateType() {
		return hibernateType;
	}

	@Override
	public void setHibernateType(Type type) {
		if ( type == null ) {
			throw new IllegalArgumentException( "Type cannot be null" );
		}
		this.hibernateType = type;
	}
}
