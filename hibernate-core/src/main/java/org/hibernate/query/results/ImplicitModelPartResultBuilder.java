/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class ImplicitModelPartResultBuilder implements ResultBuilder {
	private final NavigablePath navigablePath;
	private final String alias;
	private final ModelPart referencedModelPart;

	public ImplicitModelPartResultBuilder(
			NavigablePath navigablePath,
			String alias,
			ModelPart referencedModelPart) {
		this.navigablePath = navigablePath;
		this.alias = alias;
		this.referencedModelPart = referencedModelPart;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl impl = ResultsHelper.impl( domainResultCreationState );

		ResultsHelper.impl( domainResultCreationState ).disallowPositionalSelections();
		;
		return referencedModelPart.createDomainResult(
				navigablePath,
				impl.getFromClauseAccess().resolveTableGroup(
						navigablePath,
						np -> impl.getFromClauseAccess().getTableGroup( navigablePath.getParent() )
				),
				alias,
				domainResultCreationState
		);
	}
}
