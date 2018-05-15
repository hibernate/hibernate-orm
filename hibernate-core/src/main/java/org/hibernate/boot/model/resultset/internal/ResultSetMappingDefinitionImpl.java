/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.resultset.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.ResultSetMappingDescriptor;

/**
 * @author Steve Ebersole
 */
public class ResultSetMappingDefinitionImpl implements ResultSetMappingDefinition {
	private final String name;

	private List<Result> results;
	private List<FetchDefinitionImpl> fetches;

	public ResultSetMappingDefinitionImpl(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<Result> getResults() {
		return results == null
				? Collections.emptyList()
				: Collections.unmodifiableList( results );
	}

	@Override
	public List<FetchDefinitionImpl> getFetches() {
		return fetches == null
				? Collections.emptyList()
				: Collections.unmodifiableList( fetches );
	}

	public void addResult(Result result) {
		if ( results == null ) {
			results = new ArrayList<>();
		}

		results.add( result );
	}

	public void addFetch(FetchDefinitionImpl fetch) {
		if ( fetches == null ) {
			fetches = new ArrayList<>();
		}
		fetches.add( fetch );
	}

	@Override
	public ResultSetMappingDescriptor resolve(SessionFactoryImplementor sessionFactory) {
		final ResultSetMappingDescriptor resultSetMapping = new ResultSetMappingDescriptor( name );


		for ( Result result : results ) {
			resultSetMapping.addResultBuilder( result.generateQueryResultBuilder( sessionFactory.getMetamodel() ) );
		}

//		for ( FetchDefinitionImpl fetch : fetches ) {
//			resultSetMapping.addFetchBuilder( fetch.generateFetchBuilder( sessionFactory.getTypeConfiguration() ) );
//		}

		return resultSetMapping;
	}
}
