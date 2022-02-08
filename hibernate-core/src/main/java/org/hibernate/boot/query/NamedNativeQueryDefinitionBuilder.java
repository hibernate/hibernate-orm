/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.internal.NamedNativeQueryDefinitionImpl;

/**
 * @author Steve Ebersole
 */
public class NamedNativeQueryDefinitionBuilder extends AbstractNamedQueryBuilder<NamedNativeQueryDefinitionBuilder> {
	private String sqlString;

	private String resultSetMappingName;
	private String resultSetMappingClassName;

	private Set<String> querySpaces;

	private Map<String, String> parameterTypes;
	private Integer firstResult;
	private Integer maxResults;

	public NamedNativeQueryDefinitionBuilder(String name) {
		super( name );
	}

	public NamedNativeQueryDefinitionBuilder setSqlString(String sqlString) {
		this.sqlString = sqlString;
		return getThis();
	}

	public NamedNativeQueryDefinitionBuilder setFirstResult(Integer firstResult) {
		this.firstResult = firstResult;
		return getThis();
	}

	public NamedNativeQueryDefinitionBuilder setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
		return getThis();
	}

	public NamedNativeQueryDefinition build() {
		return new NamedNativeQueryDefinitionImpl(
				getName(),
				sqlString,
				resultSetMappingName,
				resultSetMappingClassName,
				getQuerySpaces(),
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				firstResult,
				maxResults,
				getHints()
		);
	}

	@Override
	protected NamedNativeQueryDefinitionBuilder getThis() {
		return this;
	}

	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	public NamedNativeQueryDefinitionBuilder addSynchronizedQuerySpaces(Set<String> querySpaces) {
		if ( querySpaces == null || querySpaces.isEmpty() ) {
			return this;
		}

		if ( this.querySpaces == null ) {
			this.querySpaces = new HashSet<>();
		}

		this.querySpaces.addAll( querySpaces );

		return getThis();
	}

	public NamedNativeQueryDefinitionBuilder addSynchronizedQuerySpace(String space) {
		if ( this.querySpaces == null ) {
			this.querySpaces = new HashSet<>();
		}
		this.querySpaces.add( space );
		return getThis();
	}

	public NamedNativeQueryDefinitionBuilder setQuerySpaces(Set<String> spaces) {
		this.querySpaces = spaces;
		return this;
	}

	public NamedNativeQueryDefinitionBuilder setResultSetMappingName(String resultSetMappingName) {
		this.resultSetMappingName = resultSetMappingName;
		return this;
	}

	public NamedNativeQueryDefinitionBuilder setResultSetMappingClassName(String resultSetMappingClassName) {
		this.resultSetMappingClassName = resultSetMappingClassName;
		return this;
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setFetchSize(Integer fetchSize) {
		return (NamedNativeQueryDefinitionBuilder) super.setFetchSize( fetchSize );
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setCacheable(Boolean cacheable) {
		return (NamedNativeQueryDefinitionBuilder) super.setCacheable( cacheable );
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setCacheRegion(String cacheRegion) {
		return (NamedNativeQueryDefinitionBuilder) super.setCacheRegion( cacheRegion );
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setCacheMode(CacheMode cacheMode) {
		return (NamedNativeQueryDefinitionBuilder) super.setCacheMode( cacheMode );
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setLockOptions(LockOptions lockOptions) {
		return (NamedNativeQueryDefinitionBuilder) super.setLockOptions( lockOptions );
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setTimeout(Integer timeout) {
		return (NamedNativeQueryDefinitionBuilder) super.setTimeout( timeout );
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setFlushMode(FlushMode flushMode) {
		return (NamedNativeQueryDefinitionBuilder) super.setFlushMode( flushMode );
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setReadOnly(Boolean readOnly) {
		return (NamedNativeQueryDefinitionBuilder) super.setReadOnly( readOnly );
	}

	@Override
	public NamedNativeQueryDefinitionBuilder setComment(String comment) {
		return (NamedNativeQueryDefinitionBuilder) super.setComment( comment );
	}

	public void addParameterTypeHint(String name, String type) {
		if ( parameterTypes == null ) {
			parameterTypes = new HashMap<>();
		}

		parameterTypes.put( name, type );
	}
}
