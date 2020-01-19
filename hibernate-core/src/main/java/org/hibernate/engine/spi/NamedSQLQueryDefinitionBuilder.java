/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;

public class NamedSQLQueryDefinitionBuilder extends NamedQueryDefinitionBuilder {
	private NativeSQLQueryReturn[] queryReturns;
	private Collection<String> querySpaces;
	private boolean callable;
	private String resultSetRef;

	public NamedSQLQueryDefinitionBuilder() {
	}

	public NamedSQLQueryDefinitionBuilder(String name) {
		super( name );
	}

	public NamedSQLQueryDefinitionBuilder setQueryReturns(NativeSQLQueryReturn[] queryReturns) {
		this.queryReturns = queryReturns;
		return this;
	}

	public NamedSQLQueryDefinitionBuilder setQueryReturns(List<NativeSQLQueryReturn> queryReturns) {
		if ( queryReturns != null ) {
			this.queryReturns = queryReturns.toArray( new NativeSQLQueryReturn[ queryReturns.size() ] );
		}
		else {
			this.queryReturns = null;
		}
		return this;
	}

	public NamedSQLQueryDefinitionBuilder setQuerySpaces(List<String> querySpaces) {
		this.querySpaces = querySpaces;
		return this;
	}

	public NamedSQLQueryDefinitionBuilder setQuerySpaces(Collection<String> synchronizedQuerySpaces) {
		this.querySpaces = synchronizedQuerySpaces;
		return this;
	}

	public NamedSQLQueryDefinitionBuilder addSynchronizedQuerySpace(String table) {
		if ( this.querySpaces == null ) {
			this.querySpaces = new ArrayList<String>();
		}
		this.querySpaces.add( table );
		return this;
}

	public NamedSQLQueryDefinitionBuilder setResultSetRef(String resultSetRef) {
		this.resultSetRef = resultSetRef;
		return this;
	}

	public NamedSQLQueryDefinitionBuilder setCallable(boolean callable) {
		this.callable = callable;
		return this;
	}


	@Override
	public NamedSQLQueryDefinitionBuilder setName(String name) {
		return (NamedSQLQueryDefinitionBuilder) super.setName( name );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setQuery(String query) {
		return (NamedSQLQueryDefinitionBuilder) super.setQuery( query );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setCacheable(boolean cacheable) {
		return (NamedSQLQueryDefinitionBuilder) super.setCacheable( cacheable );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setCacheRegion(String cacheRegion) {
		return (NamedSQLQueryDefinitionBuilder) super.setCacheRegion( cacheRegion );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setTimeout(Integer timeout) {
		return (NamedSQLQueryDefinitionBuilder) super.setTimeout( timeout );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setFetchSize(Integer fetchSize) {
		return (NamedSQLQueryDefinitionBuilder) super.setFetchSize( fetchSize );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setFlushMode(FlushMode flushMode) {
		return (NamedSQLQueryDefinitionBuilder) super.setFlushMode( flushMode );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setCacheMode(CacheMode cacheMode) {
		return (NamedSQLQueryDefinitionBuilder) super.setCacheMode( cacheMode );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setReadOnly(boolean readOnly) {
		return (NamedSQLQueryDefinitionBuilder) super.setReadOnly( readOnly );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setComment(String comment) {
		return (NamedSQLQueryDefinitionBuilder) super.setComment( comment );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder addParameterType(String name, String typeName) {
		return (NamedSQLQueryDefinitionBuilder) super.addParameterType( name, typeName );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setParameterTypes(Map parameterTypes) {
		return (NamedSQLQueryDefinitionBuilder) super.setParameterTypes( parameterTypes );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setLockOptions(LockOptions lockOptions) {
		// todo : maybe throw an exception here instead? since this is not valid for native-0sql queries?
		return (NamedSQLQueryDefinitionBuilder) super.setLockOptions( lockOptions );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setFirstResult(Integer firstResult) {
		return (NamedSQLQueryDefinitionBuilder) super.setFirstResult( firstResult );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setMaxResults(Integer maxResults) {
		return (NamedSQLQueryDefinitionBuilder) super.setMaxResults( maxResults );
	}

	@Override
	public NamedSQLQueryDefinitionBuilder setPassDistinctThrough(Boolean passDistinctThrough) {
		return (NamedSQLQueryDefinitionBuilder) super.setPassDistinctThrough( passDistinctThrough );
	}

	@Override
	public NamedSQLQueryDefinition createNamedQueryDefinition() {
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
				queryReturns,
				passDistinctThrough
		);
	}

	private List<String> querySpacesCopy() {
		return querySpaces == null
				? null
				: new ArrayList<String>( querySpaces );
	}
}
