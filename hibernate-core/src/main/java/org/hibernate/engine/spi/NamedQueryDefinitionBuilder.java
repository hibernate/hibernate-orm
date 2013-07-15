/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.spi;

import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;

public class NamedQueryDefinitionBuilder {
	protected String name;
	protected String query;
	protected boolean cacheable;
	protected String cacheRegion;
	protected Integer timeout;
	protected Integer fetchSize;
	protected FlushMode flushMode;
	protected CacheMode cacheMode;
	protected boolean readOnly;
	protected String comment;
	protected Map parameterTypes;
	protected LockOptions lockOptions;
	protected Integer firstResult;
	protected Integer maxResults;

	public NamedQueryDefinitionBuilder() {
	}

	public NamedQueryDefinitionBuilder(String name) {
		this.name = name;
	}

	public NamedQueryDefinitionBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public NamedQueryDefinitionBuilder setQuery(String query) {
		this.query = query;
		return this;
	}

	public NamedQueryDefinitionBuilder setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	public NamedQueryDefinitionBuilder setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion;
		return this;
	}

	public NamedQueryDefinitionBuilder setTimeout(Integer timeout) {
		this.timeout = timeout;
		return this;
	}

	public NamedQueryDefinitionBuilder setFetchSize(Integer fetchSize) {
		this.fetchSize = fetchSize;
		return this;
	}

	public NamedQueryDefinitionBuilder setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	public NamedQueryDefinitionBuilder setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	public NamedQueryDefinitionBuilder setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	public NamedQueryDefinitionBuilder setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public NamedQueryDefinitionBuilder setParameterTypes(Map parameterTypes) {
		this.parameterTypes = parameterTypes;
		return this;
	}

	public NamedQueryDefinitionBuilder setLockOptions(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	public NamedQueryDefinitionBuilder setFirstResult(Integer firstResult) {
		this.firstResult = firstResult;
		return this;
	}

	public NamedQueryDefinitionBuilder setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	public NamedQueryDefinition createNamedQueryDefinition() {
		return new NamedQueryDefinition(
				name,
				query,
				cacheable,
				cacheRegion,
				timeout,
				lockOptions,
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes,
				firstResult,
				maxResults
		);
	}
}
