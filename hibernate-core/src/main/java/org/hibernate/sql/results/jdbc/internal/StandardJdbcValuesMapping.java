/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.List;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;

/**
 * @author Steve Ebersole
 */
public class StandardJdbcValuesMapping implements JdbcValuesMapping {
	private final List<SqlSelection> sqlSelections;
	private final List<DomainResult<?>> domainResults;

	public StandardJdbcValuesMapping(
			List<SqlSelection> sqlSelections,
			List<DomainResult<?>> domainResults) {
		this.sqlSelections = sqlSelections;
		this.domainResults = domainResults;
	}

	@Override
	public List<SqlSelection> getSqlSelections() {
		return sqlSelections;
	}

	@Override
	public List<DomainResult<?>> getDomainResults() {
		return domainResults;
	}

	@Override
	public int getRowSize() {
		return sqlSelections.size();
	}

	@Override
	public List<DomainResultAssembler<?>> resolveAssemblers(AssemblerCreationState creationState) {
		final List<DomainResultAssembler<?>> assemblers = CollectionHelper.arrayList( domainResults.size() );

		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < domainResults.size(); i++ ) {
			final DomainResultAssembler<?> resultAssembler = domainResults.get( i )
					.createResultAssembler( (InitializerParent) null, creationState );

			assemblers.add( resultAssembler );
		}

		return assemblers;
	}
}
