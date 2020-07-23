/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;

/**
 * Implementation of JdbcValuesMapping for native / procedure queries
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingImpl implements JdbcValuesMapping {
	private final List<SqlSelection> sqlSelections;
	private final List<DomainResult<?>> domainResults;

	public JdbcValuesMappingImpl(
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
	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<DomainResult> getDomainResults() {
		return (List) domainResults;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public List<DomainResultAssembler> resolveAssemblers(AssemblerCreationState creationState) {
		final List<DomainResultAssembler> assemblers = new ArrayList<>( domainResults.size() );
		domainResults.forEach(
				domainResult -> assemblers.add( domainResult.createResultAssembler( creationState ) )
		);
		return assemblers;
	}
}
