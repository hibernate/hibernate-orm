/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import javax.persistence.ParameterMode;

import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.query.internal.QueryParameterImpl;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterImpl<T> extends QueryParameterImpl<T> implements ProcedureParameterImplementor<T> {
	private ParameterRegistrationImplementor<T> nativeParamRegistration;

	public ProcedureParameterImpl(ParameterRegistrationImplementor<T> nativeParamRegistration) {
		super( nativeParamRegistration.getHibernateType() );
		this.nativeParamRegistration = nativeParamRegistration;
	}

	@Override
	public ParameterMode getMode() {
		return nativeParamRegistration.getMode();
	}

	@Override
	public boolean isPassNullsEnabled() {
		return nativeParamRegistration.isPassNullsEnabled();
	}

	@Override
	public void enablePassingNulls(boolean enabled) {
		nativeParamRegistration.enablePassingNulls( enabled );
	}

	@Override
	public boolean isJpaPositionalParameter() {
		return false;
	}

	@Override
	public String getName() {
		return nativeParamRegistration.getName();
	}

	@Override
	public Integer getPosition() {
		return nativeParamRegistration.getPosition();
	}

	@Override
	public ParameterRegistrationImplementor<T> getNativeParameterRegistration() {
		return nativeParamRegistration;
	}
}
