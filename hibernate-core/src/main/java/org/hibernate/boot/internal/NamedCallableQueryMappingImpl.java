/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.spi.AbstractNamedQueryMapping;
import org.hibernate.boot.spi.NamedCallableQueryMapping;
import org.hibernate.boot.spi.NamedQueryParameterMapping;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.internal.NamedCallableQueryMementoImpl;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.procedure.internal.ProcedureParameterImpl;

/**
 * @author Steve Ebersole
 */
public class NamedCallableQueryMappingImpl extends AbstractNamedQueryMapping implements NamedCallableQueryMapping {
	private final String callableName;
	private final List<String> resultSetMappingNames;
	private final List<Class> resultClasses;
	private final Set<String> querySpaces;

	public NamedCallableQueryMappingImpl(
			String name,
			String callableName,
			List<NamedQueryParameterMapping> parameterMappings,
			List<Class> resultClasses,
			List<String> resultSetMappingNames,
			Set<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String,Object> hints) {
		super(
				name,
				parameterMappings,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				hints
		);

		this.callableName = callableName;
		this.resultSetMappingNames = resultSetMappingNames;
		this.resultClasses = resultClasses;
		this.querySpaces = querySpaces;
	}

	@Override
	public NamedCallableQueryMemento resolve(SessionFactoryImplementor factory) {
		return new NamedCallableQueryMementoImpl(
				getName(),
				callableName,
				ParameterStrategy.UNKNOWN,
				resolveParameterMappings( factory ),
				resultClasses,
				resultSetMappingNames,
				querySpaces,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				getHints()
		);
	}

	@Override
	protected List<? extends NamedCallableQueryMementoImpl.CallableParameterMemento> resolveParameterMappings(SessionFactoryImplementor factory) {
		//noinspection unchecked
		return (List) super.resolveParameterMappings( factory );
	}

	public static class Builder extends AbstractBuilder<Builder> {
		private String callableName;

		private List<Class> resultClasses;
		private List<String> resultSetMappingNames;

		private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
		private List<NamedCallableQueryParameterMapping> parameterMappings;

		public Builder(String name) {
			super( name );
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public Builder consume(StoredProcedureParameter[] parameters) {
			if ( parameters != null && parameters.length > 0 ) {
				for ( StoredProcedureParameter parameter : parameters ) {
					consume( parameter );
				}
			}
			return getThis();
		}

		private void consume(StoredProcedureParameter parameter) {
			if ( BinderHelper.isEmptyAnnotationValue( parameter.name() ) ) {
				consumeNamedParameter( parameter.name(), parameter.type(), parameter.mode() );
			}
			if ( parameter.name() != null ) {
			}
		}

		private void consumeNamedParameter(String name, Class type, ParameterMode mode) {
			if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
				throw new IllegalArgumentException(
						"Named queries cannot mix named and positional parameters: " + getName()
				);
			}

			parameterStrategy = ParameterStrategy.NAMED;
			final NamedCallableQueryParameterMapping namedParameter = new NamedCallableQueryParameterMapping() {
				@Override
				public ParameterMemento resolve(SessionFactoryImplementor factory) {
					return session -> new ProcedureParameterImpl(
							label,
							mode,
							javaType,
							factory.getMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( javaType ),
							factory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled()
					);
				}
			};
			parameterMappings.add( namedParameter );
		}

		@Override
		protected NamedCallableQueryParameterMapping createPositionalParameter(int label, Class javaType, ParameterMode mode) {
			return
		}

		@Override
		protected NamedCallableQueryParameterMapping createNamedParameter(String name, Class javaType, ParameterMode mode) {
			//noinspection Convert2Lambda
			return new NamedQueryParameterMapping() {
				@Override
				@SuppressWarnings("unchecked")
				public ParameterMemento resolve(SessionFactoryImplementor factory) {
					return session -> new ProcedureParameterImpl(
							name,
							mode,
							javaType,
							factory.getMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( javaType ),
							factory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled()
					);
				}
			};
		}

		public NamedCallableQueryMapping build() {
			return new NamedCallableQueryMappingImpl(
					getName(),
					callableName,
					getParameterMappings(),
					resultClasses,
					resultSetMappingNames,
					getQuerySpaces(),
					getCacheable(),
					getCacheRegion(),
					getCacheMode(),
					getFlushMode(),
					getReadOnly(),
					getLockOptions(),
					getTimeout(),
					getFetchSize(),
					getComment(),
					getHints()
			);
		}

		public Builder setCallableName(String callableName) {
			this.callableName = callableName;
			return this;
		}
	}

	private static class NamedCallableQueryParameterMapping implements NamedQueryParameterMapping {

		@Override
		public ParameterMemento resolve(SessionFactoryImplementor factory) {
			return null;
		}
	}
}
