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

import org.hibernate.HibernateException;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.exec.spi.InFlightJdbcCall;
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
public class JdbcCallImpl implements InFlightJdbcCall {
	private final String callableName;
	private final ParameterStrategy parameterStrategy;

	private JdbcCallFunctionReturn functionReturn;
	private List<JdbcCallParameterRegistration> parameterRegistrations;
	private List<JdbcParameterBinder> parameterBinders;
	private List<JdbcCallParameterExtractor> parameterExtractors;
	private List<JdbcCallRefCursorExtractor> refCursorExtractors;
	private List<SqlSelection> sqlSelections;
	private List<Return> queryReturns;

	public JdbcCallImpl(String callableName, ParameterStrategy parameterStrategy) {
		this.callableName = callableName;
		this.parameterStrategy = parameterStrategy;
	}

	@Override
	public String getSql() {
		return callableName;
	}

	@Override
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

	@Override
	public List<SqlSelection> getSqlSelections() {
		return sqlSelections;
	}

	@Override
	public List<Return> getReturns() {
		return queryReturns;
	}

	@Override
	public void setFunctionReturn(JdbcCallFunctionReturn functionReturn) {
		this.functionReturn = functionReturn;
	}

	@Override
	public void addParameterRegistration(JdbcCallParameterRegistration registration) {
		if ( parameterRegistrations == null ) {
			parameterRegistrations = new ArrayList<>();
		}
		parameterRegistrations.add( registration );

		switch ( registration.getParameterMode() ) {
			case REF_CURSOR: {
				addRefCursorExtractor( registration.getRefCursorExtractor() );
				break;
			}
			case IN: {
				addParameterBinder( registration.getParameterBinder() );
				break;
			}
			case INOUT: {
				addParameterBinder( registration.getParameterBinder() );
				addParameterExtractor( registration.getParameterExtractor() );
				break;
			}
			case OUT: {
				addParameterExtractor( registration.getParameterExtractor() );
				break;
			}
			default: {
				throw new HibernateException( "Unexpected ParameterMode : " +registration.getParameterMode() );
			}
		}
	}

	private void addParameterBinder(JdbcParameterBinder binder) {
		if ( parameterBinders == null ) {
			parameterBinders = new ArrayList<>();
		}
		parameterBinders.add( binder );
	}

	private void addParameterExtractor(JdbcCallParameterExtractor extractor) {
		if ( parameterExtractors == null ) {
			parameterExtractors = new ArrayList<>();
		}
		parameterExtractors.add( extractor );
	}

	private void addRefCursorExtractor(JdbcCallRefCursorExtractor extractor) {
		if ( refCursorExtractors == null ) {
			refCursorExtractors = new ArrayList<>();
		}
		refCursorExtractors.add( extractor );
	}

	@Override
	public void addSqlSelection(SqlSelection sqlSelection) {
		if ( sqlSelections == null ) {
			sqlSelections = new ArrayList<>();
		}
		sqlSelections.add( sqlSelection );
	}

	@Override
	public void addQueryReturn(Return queryReturn) {
		if ( queryReturns == null ) {
			queryReturns  = new ArrayList<>();
		}
		queryReturns.add( queryReturn );
	}
}
