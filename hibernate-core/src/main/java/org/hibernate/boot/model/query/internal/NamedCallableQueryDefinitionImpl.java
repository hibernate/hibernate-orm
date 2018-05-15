/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.query.spi.NamedCallableQueryDefinition;
import org.hibernate.boot.model.query.spi.ParameterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.internal.ProcedureParameterImpl;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.named.internal.NamedCallableQueryMementoImpl;
import org.hibernate.query.named.spi.NamedCallableQueryMemento;
import org.hibernate.query.named.spi.ParameterMemento;

/**
 * @author Steve Ebersole
 */
public class NamedCallableQueryDefinitionImpl
		extends AbstractNamedQueryDefinition
		implements NamedCallableQueryDefinition {

	public static final Class[] EMPTY_CLASSES = new Class[0];
	public static final String[] EMPTY_NAMES = new String[0];

	private final String callableName;
	private final Class[] resultClasses;
	private final String[] resultSetMappingNames;
	private final Set<String> querySpaces;

	public NamedCallableQueryDefinitionImpl(
			String name,
			String callableName,
			List<ParameterDefinition> parameterDefinitions,
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
				parameterDefinitions,
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

		this.resultClasses = resultClasses == null || resultClasses.isEmpty()
				? EMPTY_CLASSES
				: resultClasses.toArray( new Class[0] );

		this.resultSetMappingNames = resultSetMappingNames == null || resultSetMappingNames.isEmpty()
				? EMPTY_NAMES
				: resultSetMappingNames.toArray( new String[0] );

		this.querySpaces = querySpaces;
	}

	@Override
	public NamedCallableQueryMemento resolve(SessionFactoryImplementor factory) {
		return new NamedCallableQueryMementoImpl(
				getName(),
				callableName,
				ParameterStrategy.UNKNOWN,
				resolveParameterDescriptors( factory ),
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

	public  static class Builder extends AbstractBuilder<Builder> {
		private String callableName;

		private List<Class> resultClasses;
		private List<String> resultSetMappingNames;

		public Builder(String name) {
			super( name );
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		@Override
		protected ParameterDefinition createPositionalParameter(int label, Class javaType, ParameterMode mode) {
			//noinspection Convert2Lambda
			return new ParameterDefinition() {
				@Override
				@SuppressWarnings("unchecked")
				public ParameterMemento resolve(SessionFactoryImplementor factory) {
					return session -> new ProcedureParameterImpl(
							label,
							mode,
							javaType,
							factory.getTypeConfiguration().getBasicTypeRegistry().getBasicType( javaType ),
							factory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled()
					);
				}
			};
		}

		@Override
		protected ParameterDefinition createNamedParameter(String name, Class javaType, ParameterMode mode) {
			//noinspection Convert2Lambda
			return new ParameterDefinition() {
				@Override
				@SuppressWarnings("unchecked")
				public ParameterMemento resolve(SessionFactoryImplementor factory) {
					return session -> new ProcedureParameterImpl(
							name,
							mode,
							javaType,
							factory.getTypeConfiguration().getBasicTypeRegistry().getBasicType( javaType ),
							factory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled()
					);
				}
			};
		}

		public NamedCallableQueryDefinition build() {
			return new NamedCallableQueryDefinitionImpl(
					getName(),
					callableName,
					getParameterDescriptors(),
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
}
