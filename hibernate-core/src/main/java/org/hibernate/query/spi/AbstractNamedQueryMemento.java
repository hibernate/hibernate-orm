/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.ParameterMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.internal.Util;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public abstract class AbstractNamedQueryMemento implements NamedQueryMemento {
	private final String name;

	private final List<? extends ParameterMemento> parameterMementos;

	private final Boolean cacheable;
	private final String cacheRegion;
	private final CacheMode cacheMode;

	private final FlushMode flushMode;
	private final Boolean readOnly;

	private final LockOptions lockOptions;

	private final Integer timeout;
	private final Integer fetchSize;

	private final String comment;

	private final Map<String, Object> hints;

	public AbstractNamedQueryMemento(
			String name,
			List<? extends ParameterMemento> parameterMementos,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, Object> hints) {
		this.name = name;
		this.parameterMementos = parameterMementos;
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		this.cacheMode = cacheMode;
		this.flushMode = flushMode;
		this.readOnly = readOnly;
		this.lockOptions = lockOptions;
		this.timeout = timeout;
		this.fetchSize = fetchSize;
		this.comment = comment;
		this.hints = hints;
	}

	@Override
	public String getName() {
		return name;
	}

	public List<ParameterMemento> getParameterMementos() {
		return parameterMementos;
	}

	@Override
	public Boolean getCacheable() {
		return cacheable;
	}

	@Override
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public FlushMode getFlushMode() {
		return flushMode;
	}

	@Override
	public Boolean getReadOnly() {
		return readOnly;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public Integer getTimeout() {
		return timeout;
	}

	@Override
	public Integer getFetchSize() {
		return fetchSize;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}

	protected Map<String, Object> getHintsCopy() {
		return Util.copy( hints );
	}

	protected void applyBaseOptions(QueryImplementor query, SharedSessionContractImplementor session) {
		getHints().forEach( query::setHint );

		if ( cacheable != null ) {
			query.setCacheable( cacheable );
		}

		if ( cacheRegion != null ) {
			query.setCacheRegion( cacheRegion );
		}

		if ( cacheMode != null ) {
			query.setCacheMode( cacheMode );
		}

		if ( flushMode != null ) {
			query.setHibernateFlushMode( flushMode );
		}

		if ( readOnly != null ) {
			query.setReadOnly( readOnly );
		}

		if ( lockOptions != null ) {
			query.setLockOptions( lockOptions );
		}

		if ( timeout != null ) {
			query.setTimeout( timeout );
		}

		if ( fetchSize != null ) {
			query.setFetchSize( fetchSize );
		}

		if ( comment != null ) {
			query.setComment( comment );
		}
	}

	protected static abstract class AbstractBuilder<T extends AbstractBuilder> {
		private final String name;

		private List<ParameterDefinition> parameterDescriptors;

		private Set<String> querySpaces;
		private Boolean cacheable;
		private String cacheRegion;
		private CacheMode cacheMode;

		private FlushMode flushMode;
		private Boolean readOnly;

		private LockOptions lockOptions;

		private Integer timeout;
		private Integer fetchSize;

		private String comment;

		private Map<String,Object> hints;

		public AbstractBuilder(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		protected abstract T getThis();

		public T addParameter(Class javaType, ParameterMode mode) {
			return addParameter(
					createPositionalParameter(
							parameterDescriptors.size() + 1,
							javaType,
							mode
					)
			);
		}

		protected abstract ParameterDefinition createPositionalParameter(int i, Class javaType, ParameterMode mode);

		public T addParameter(ParameterDefinition parameterDefinition) {
			if ( parameterDescriptors == null ) {
				parameterDescriptors = new ArrayList<>();
			}

			parameterDescriptors.add( parameterDefinition );

			return getThis();
		}

		public T addParameter(String name, Class javaType, ParameterMode mode) {
			if ( parameterDescriptors == null ) {
				parameterDescriptors = new ArrayList<>();
			}

			parameterDescriptors.add( createNamedParameter( name, javaType, mode ) );

			return getThis();
		}

		protected abstract ParameterDefinition createNamedParameter(String name, Class javaType, ParameterMode mode);


		public T addQuerySpaces(Set<String> querySpaces) {
			if ( querySpaces == null || querySpaces.isEmpty() ) {
				return getThis();
			}

			if ( this.querySpaces == null ) {
				this.querySpaces = new HashSet<>();
			}
			this.querySpaces.addAll( querySpaces );
			return getThis();
		}

		public T addQuerySpace(String space) {
			if ( this.querySpaces == null ) {
				this.querySpaces = new HashSet<>();
			}
			this.querySpaces.add( space );
			return getThis();
		}

		public T setCacheable(Boolean cacheable) {
			this.cacheable = cacheable;
			return getThis();
		}

		public T setCacheRegion(String cacheRegion) {
			this.cacheRegion = cacheRegion;
			return getThis();
		}

		public T setCacheMode(CacheMode cacheMode) {
			this.cacheMode = cacheMode;
			return getThis();
		}

		public T setLockOptions(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
			return getThis();
		}

		public T setTimeout(Integer timeout) {
			this.timeout = timeout;
			return getThis();
		}

		public T setFlushMode(FlushMode flushMode) {
			this.flushMode = flushMode;
			return getThis();
		}

		public T setReadOnly(Boolean readOnly) {
			this.readOnly = readOnly;
			return getThis();
		}

		public T setFetchSize(Integer fetchSize) {
			this.fetchSize = fetchSize;
			return getThis();
		}

		public T setComment(String comment) {
			this.comment = comment;
			return getThis();
		}

		public Set<String> getQuerySpaces() {
			return querySpaces;
		}

		public Boolean getCacheable() {
			return cacheable;
		}

		public String getCacheRegion() {
			return cacheRegion;
		}

		public CacheMode getCacheMode() {
			return cacheMode;
		}

		public FlushMode getFlushMode() {
			return flushMode;
		}

		public Boolean getReadOnly() {
			return readOnly;
		}

		public LockOptions getLockOptions() {
			return lockOptions;
		}

		public Integer getTimeout() {
			return timeout;
		}

		public Integer getFetchSize() {
			return fetchSize;
		}

		public String getComment() {
			return comment;
		}

		protected List<ParameterDefinition> getParameterDescriptors() {
			return parameterDescriptors;
		}

		public void addHint(String name, Object value) {
			if ( hints == null ) {
				hints = new HashMap<>();
			}
			hints.put( name, value );
		}

		public Map<String, Object> getHints() {
			return hints;
		}
	}
}
