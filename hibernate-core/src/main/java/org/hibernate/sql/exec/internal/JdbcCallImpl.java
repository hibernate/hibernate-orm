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
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;
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

	private final JdbcCallFunctionReturn functionReturn;
	private final List<JdbcCallParameterRegistration> parameterRegistrations;
	private final List<JdbcParameterBinder> parameterBinders;
	private final List<JdbcCallParameterExtractor> parameterExtractors;
	private final List<JdbcCallRefCursorExtractor> refCursorExtractors;

	private final List<ResultSetMappingDescriptor> resultSetMappings;

	public JdbcCallImpl(Builder builder) {
		this.callableName = builder.callableName;

		this.functionReturn = builder.functionReturn;

		this.parameterRegistrations = builder.parameterRegistrations == null
				? Collections.emptyList()
				: Collections.unmodifiableList( builder.parameterRegistrations );
		this.parameterBinders = builder.parameterBinders == null
				? Collections.emptyList()
				: Collections.unmodifiableList( builder.parameterBinders );
		this.parameterExtractors = builder.parameterExtractors == null
				? Collections.emptyList()
				: Collections.unmodifiableList( builder.parameterExtractors );
		this.refCursorExtractors = builder.refCursorExtractors == null
				? Collections.emptyList()
				: Collections.unmodifiableList( builder.refCursorExtractors );

		this.resultSetMappings = builder.resultSetMappings == null
				? Collections.emptyList()
				: Collections.unmodifiableList( builder.resultSetMappings );
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
	public Set<String> getAffectedTableNames() {
		throw new NotYetImplementedFor6Exception();
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
	public List<ResultSetMappingDescriptor> getResultSetMappings() {
		return resultSetMappings == null ? Collections.emptyList() : Collections.unmodifiableList( resultSetMappings );
	}

	public static class Builder {
		private final String callableName;
		private final ParameterStrategy parameterStrategy;

		private JdbcCallFunctionReturn functionReturn;

		private List<JdbcCallParameterRegistration> parameterRegistrations;
		private List<JdbcParameterBinder> parameterBinders;
		private List<JdbcCallParameterExtractor> parameterExtractors;
		private List<JdbcCallRefCursorExtractor> refCursorExtractors;

		private List<ResultSetMappingDescriptor> resultSetMappings;

		public Builder(String callableName, ParameterStrategy parameterStrategy) {
			this.callableName = callableName;
			this.parameterStrategy = parameterStrategy;
		}

		public JdbcCall buildJdbcCall() {
			return new JdbcCallImpl( this );
		}

		public void setFunctionReturn(JdbcCallFunctionReturn functionReturn) {
			this.functionReturn = functionReturn;
		}

		public void addParameterRegistration(JdbcCallParameterRegistration registration) {
			if ( parameterRegistrations == null ) {
				parameterRegistrations = new ArrayList<>();
			}

			// todo (6.0) : add validation based on ParameterStrategy

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

		public void addResultSetMapping(ResultSetMappingDescriptor resultSetMapping) {
			if ( resultSetMappings == null ) {
				resultSetMappings = new ArrayList<>();
			}

			resultSetMappings.add( resultSetMapping );
		}
	}
}
