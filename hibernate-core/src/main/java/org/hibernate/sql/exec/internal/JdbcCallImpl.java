/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.sql.exec.spi.JdbcCall;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * Models the actual call, allowing iterative building of the parts.
 *
 * @author Steve Ebersole
 */
public class JdbcCallImpl implements JdbcCall {
	private final String callableName;
	private final ParameterStrategy parameterStrategy;

	private JdbcCallFunctionReturn functionReturn;
	private List<JdbcCallParameterRegistration> parameterRegistrations;
	private List<JdbcParameterBinder> parameterBinders;
	private List<JdbcCallParameterExtractor> parameterExtractors;
	private List<JdbcCallRefCursorExtractor> refCursorExtractors;

	public JdbcCallImpl(String callableName, ParameterStrategy parameterStrategy) {
		this.callableName = callableName;
		this.parameterStrategy = parameterStrategy;
	}

	@Override
	public String getSql() {
		return callableName;
	}

	public JdbcCallFunctionReturn getFunctionReturn() {
		return functionReturn;
	}

	@Override
	public List<JdbcCallParameterRegistration> getParameterRegistrations() {
		return parameterRegistrations == null ? Collections.emptyList() : parameterRegistrations;
	}

	@Override
	public List<JdbcParameterBinder> getParameterBinders() {
		return parameterBinders == null ? Collections.emptyList() : parameterBinders;
	}

	@Override
	public List<JdbcCallParameterExtractor> getParameterExtractors() {
		return parameterExtractors == null ? Collections.emptyList() : parameterExtractors;
	}

	@Override
	public List<JdbcCallRefCursorExtractor> getCallRefCursorExtractors() {
		return refCursorExtractors == null ? Collections.emptyList() : refCursorExtractors;
	}

	public void setFunctionReturn(JdbcCallFunctionReturn functionReturn) {
		this.functionReturn = functionReturn;
	}

	public void addParameterRegistration(JdbcCallParameterRegistration registration) {
		if ( parameterRegistrations == null ) {
			parameterRegistrations = new ArrayList<>();
		}
		parameterRegistrations.add( registration );
	}

	public void addParameterBinder(JdbcParameterBinder binder) {
		if ( parameterBinders == null ) {
			parameterBinders = new ArrayList<>();
		}
		parameterBinders.add( binder );
	}

	public void addParameterExtractor(JdbcCallParameterExtractor extractor) {
		if ( parameterExtractors == null ) {
			parameterExtractors = new ArrayList<>();
		}
		parameterExtractors.add( extractor );
	}

	public void addRefCursorExtractor(JdbcCallRefCursorExtractor extractor) {
		if ( refCursorExtractors == null ) {
			refCursorExtractors = new ArrayList<>();
		}
		refCursorExtractors.add( extractor );
	}

	public String toCallString(SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getCallableStatementSupport().renderCallableStatement(
				callableName,
				parameterStrategy,
				getFunctionReturn(),
				getParameterRegistrations(),
				session
		);
	}
}
