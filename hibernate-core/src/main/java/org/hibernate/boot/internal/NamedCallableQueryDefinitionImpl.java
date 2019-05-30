/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AbstractNamedQueryDefinition;
import org.hibernate.boot.spi.NamedCallableQueryDefinition;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.procedure.internal.NamedCallableQueryMementoImpl;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.procedure.internal.ProcedureParameterImpl;

/**
 * @author Steve Ebersole
 */
public class NamedCallableQueryDefinitionImpl
		extends AbstractNamedQueryDefinition implements NamedCallableQueryDefinition {
	private static final Class[] NO_CLASSES = new Class[0];

	private final String callableName;
	private final List<ParameterMapping> parameterMappings;
	private final List<String> resultSetMappingNames;
	private final List<String> resultSetMappingClassNames;
	private final Set<String> querySpaces;

	public NamedCallableQueryDefinitionImpl(
			String name,
			String callableName,
			List<ParameterMapping> parameterMappings,
			List<String> resultSetMappingNames,
			List<String> resultSetMappingClassNames,
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
		this.parameterMappings = parameterMappings;
		this.resultSetMappingNames = resultSetMappingNames;
		this.resultSetMappingClassNames = resultSetMappingClassNames;
		this.querySpaces = querySpaces;
	}

	@Override
	public NamedCallableQueryMemento resolve(SessionFactoryImplementor factory) {
		return new NamedCallableQueryMementoImpl(
				getRegistrationName(),
				callableName,
				ParameterStrategy.UNKNOWN,
				resolveParameterMappings( factory ),
				(String[]) resultSetMappingNames.toArray(),
				toResultClasses( resultSetMappingClassNames, factory ),
				querySpaces,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				getHints()
		);
	}

	protected List<NamedCallableQueryMemento.ParameterMemento> resolveParameterMappings(SessionFactoryImplementor factory) {
		if ( parameterMappings == null || parameterMappings.isEmpty() ) {
			return Collections.emptyList();
		}

		final ArrayList<NamedCallableQueryMemento.ParameterMemento> mementos = CollectionHelper.arrayList( parameterMappings.size() );
		parameterMappings.forEach( mapping -> mementos.add( mapping.resolve( factory ) ) );
		return mementos;
	}

	private static Class<?>[] toResultClasses(List<String> classNames, SessionFactoryImplementor factory) {
		if ( classNames == null || classNames.isEmpty() ) {
			return NO_CLASSES;
		}

		final ClassLoaderService classLoaderService = factory.getServiceRegistry().getService( ClassLoaderService.class );

		final Class<?>[] classes = new Class[ classNames.size() ];
		int i = 0;
		for ( String className : classNames ) {
			try {
				classes[ i++ ] = classLoaderService.classForName( className );
			}
			catch (Exception e) {
				throw new HibernateException( "Could not resolve class name given as procedure-call result class: " + className, e );
			}
		}

		return classes;
	}

	public static class Builder extends AbstractBuilder<Builder> {
		private String callableName;

		private List<String> resultSetMappingClassNames;
		private List<String> resultSetMappingNames;

		private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
		private List<ParameterMapping> parameterMappings;

		public Builder(String name) {
			super( name );
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public Builder setCallableName(String callableName) {
			this.callableName = callableName;
			return this;
		}

		public Builder setResultSetMappingClassNames(List<String> resultSetMappingClassNames) {
			this.resultSetMappingClassNames = resultSetMappingClassNames;
			return this;
		}

		public Builder setResultSetMappingNames(List<String> resultSetMappingNames) {
			this.resultSetMappingNames = resultSetMappingNames;
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
			else {
				consumePositionalParameter( parameter.type(), parameter.mode() );
			}
		}

		private void consumeNamedParameter(String name, Class javaType, ParameterMode mode) {
			if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
				throw new IllegalArgumentException(
						"Named queries cannot mix named and positional parameters: " + getName()
				);
			}

			parameterStrategy = ParameterStrategy.NAMED;

			parameterMappings.add(
					(ParameterMapping) factory -> session -> new ProcedureParameterImpl(
							name,
							mode,
							javaType,
							factory.getMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( javaType ),
							factory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled()
					)
			);
		}

		private void consumePositionalParameter(Class javaType, ParameterMode mode) {
			if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
				throw new IllegalArgumentException(
						"Named queries cannot mix named and positional parameters: " + getName()
				);
			}

			parameterStrategy = ParameterStrategy.POSITIONAL;

			parameterMappings.add(
					(ParameterMapping) factory -> session -> new ProcedureParameterImpl(
							parameterMappings.size(),
							mode,
							javaType,
							factory.getMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( javaType ),
							factory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled()
					)

		}

		public NamedCallableQueryDefinition build() {
			return new NamedCallableQueryDefinitionImpl(
					getName(),
					callableName,
					parameterMappings,
					resultSetMappingClassNames,
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
	}
}
