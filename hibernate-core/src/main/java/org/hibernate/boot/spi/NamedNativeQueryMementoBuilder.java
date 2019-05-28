/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

/**
 * Delegate used in creating named native-sql query mementos for queries defined in
 * annotations, hbm.xml or orm.xml
 */
public class NamedNativeQueryMementoBuilder extends NamedHqlQueryMemento.Builder {
	private NativeSQLQueryReturn[] queryReturns;
	private Collection<String> querySpaces;
	private boolean callable;
	private String resultSetRef;

	public NamedNativeQueryMementoBuilder() {
	}

	public NamedNativeQueryMementoBuilder(String name) {
		super( name );
	}

	public NamedNativeQueryMementoBuilder setQueryReturns(NativeSQLQueryReturn[] queryReturns) {
		this.queryReturns = queryReturns;
		return this;
	}

	public NamedNativeQueryMementoBuilder setQueryReturns(List<NativeSQLQueryReturn> queryReturns) {
		if ( queryReturns != null ) {
			this.queryReturns = queryReturns.toArray( new NativeSQLQueryReturn[ queryReturns.size() ] );
		}
		else {
			this.queryReturns = null;
		}
		return this;
	}

	public NamedNativeQueryMementoBuilder setQuerySpaces(List<String> querySpaces) {
		this.querySpaces = querySpaces;
		return this;
	}

	public NamedNativeQueryMementoBuilder setQuerySpaces(Collection<String> synchronizedQuerySpaces) {
		this.querySpaces = synchronizedQuerySpaces;
		return this;
	}

	public NamedNativeQueryMementoBuilder addSynchronizedQuerySpace(String table) {
		if ( this.querySpaces == null ) {
			this.querySpaces = new ArrayList<String>();
		}
		this.querySpaces.add( table );
		return this;
}

	public NamedNativeQueryMementoBuilder setResultSetRef(String resultSetRef) {
		this.resultSetRef = resultSetRef;
		return this;
	}

	public NamedNativeQueryMementoBuilder setCallable(boolean callable) {
		this.callable = callable;
		return this;
	}


	@Override
	public NamedNativeQueryMementoBuilder setName(String name) {
		return (NamedNativeQueryMementoBuilder) super.setName( name );
	}

	@Override
	public NamedNativeQueryMementoBuilder setQuery(String query) {
		return (NamedNativeQueryMementoBuilder) super.setQuery( query );
	}

	@Override
	public NamedNativeQueryMementoBuilder setCacheable(boolean cacheable) {
		return (NamedNativeQueryMementoBuilder) super.setCacheable( cacheable );
	}

	@Override
	public NamedNativeQueryMementoBuilder setCacheRegion(String cacheRegion) {
		return (NamedNativeQueryMementoBuilder) super.setCacheRegion( cacheRegion );
	}

	@Override
	public NamedNativeQueryMementoBuilder setTimeout(Integer timeout) {
		return (NamedNativeQueryMementoBuilder) super.setTimeout( timeout );
	}

	@Override
	public NamedNativeQueryMementoBuilder setFetchSize(Integer fetchSize) {
		return (NamedNativeQueryMementoBuilder) super.setFetchSize( fetchSize );
	}

	@Override
	public NamedNativeQueryMementoBuilder setFlushMode(FlushMode flushMode) {
		return (NamedNativeQueryMementoBuilder) super.setFlushMode( flushMode );
	}

	@Override
	public NamedNativeQueryMementoBuilder setCacheMode(CacheMode cacheMode) {
		return (NamedNativeQueryMementoBuilder) super.setCacheMode( cacheMode );
	}

	@Override
	public NamedNativeQueryMementoBuilder setReadOnly(boolean readOnly) {
		return (NamedNativeQueryMementoBuilder) super.setReadOnly( readOnly );
	}

	@Override
	public NamedNativeQueryMementoBuilder setComment(String comment) {
		return (NamedNativeQueryMementoBuilder) super.setComment( comment );
	}

	@Override
	public NamedNativeQueryMementoBuilder addParameterType(String name, String typeName) {
		return (NamedNativeQueryMementoBuilder) super.addParameterType( name, typeName );
	}

	@Override
	public NamedNativeQueryMementoBuilder setParameterTypes(Map parameterTypes) {
		return (NamedNativeQueryMementoBuilder) super.setParameterTypes( parameterTypes );
	}

	@Override
	public NamedNativeQueryMementoBuilder setLockOptions(LockOptions lockOptions) {
		// todo : maybe throw an exception here instead? since this is not valid for native-0sql queries?
		return (NamedNativeQueryMementoBuilder) super.setLockOptions( lockOptions );
	}

	@Override
	public NamedNativeQueryMementoBuilder setFirstResult(Integer firstResult) {
		return (NamedNativeQueryMementoBuilder) super.setFirstResult( firstResult );
	}

	@Override
	public NamedNativeQueryMementoBuilder setMaxResults(Integer maxResults) {
		return (NamedNativeQueryMementoBuilder) super.setMaxResults( maxResults );
	}

	@Override
	public NamedNativeQueryMemento createNamedQueryDefinition() {
		return new NamedSQLQueryDefinition(
				name,
				query,
				cacheable,
				cacheRegion,
				timeout,
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes,
				firstResult,
				maxResults,
				resultSetRef,
				querySpacesCopy(),
				callable,
				queryReturns
		);
	}

	private List<String> querySpacesCopy() {
		return querySpaces == null
				? null
				: new ArrayList<String>( querySpaces );
	}
}
