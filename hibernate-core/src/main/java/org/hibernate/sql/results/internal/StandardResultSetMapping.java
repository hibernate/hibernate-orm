/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class StandardResultSetMapping implements ResultSetMapping {
	private final Set<SqlSelection> sqlSelections;
	private final List<DomainResult> domainResults;

	public StandardResultSetMapping(
			Set<SqlSelection> sqlSelections,
			List<DomainResult> domainResults) {
		this.sqlSelections = sqlSelections;
		this.domainResults = domainResults;
	}

	@Override
	public Set<SqlSelection> getSqlSelections() {
		return sqlSelections;
	}

	@Override
	public List<DomainResult> getDomainResults() {
		return domainResults;
	}

	@Override
	public List<DomainResultAssembler> resolveAssemblers(
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState,
			AssemblerCreationContext creationContext) {
		final List<DomainResultAssembler> assemblers = CollectionHelper.arrayList( domainResults.size() );

		for ( DomainResult domainResult : domainResults ) {
			final DomainResultAssembler resultAssembler = domainResult.createResultAssembler(
					initializerConsumer,
					creationState,
					creationContext
			);

			assemblers.add( resultAssembler );
		}

		return assemblers;
	}
}
