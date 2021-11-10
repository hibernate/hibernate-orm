/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultBuilderEmbeddable;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class ImplicitModelPartResultBuilderEmbeddable
		implements ImplicitModelPartResultBuilder, ResultBuilderEmbeddable {

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart modelPart;

	public ImplicitModelPartResultBuilderEmbeddable(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart modelPart) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
	}

	@Override
	public Class<?> getJavaType() {
		return modelPart.getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public EmbeddableResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );
		creationStateImpl.disallowPositionalSelections();

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				np -> {
					if ( navigablePath.getParent() == null ) {
						throw new IllegalStateException(
								"Could not determine LHS for implicit embeddable result builder - " + navigablePath
						);
					}

					final TableGroup parentTableGroup = creationStateImpl
							.getFromClauseAccess()
							.getTableGroup( navigablePath.getParent() );

					final TableGroupJoin tableGroupJoin = modelPart.createTableGroupJoin(
							navigablePath,
							parentTableGroup,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							creationStateImpl
					);

					parentTableGroup.addTableGroupJoin( tableGroupJoin );

					return tableGroupJoin.getJoinedGroup();
				}
		);

		return (EmbeddableResult) modelPart.createDomainResult(
				navigablePath,
				tableGroup,
				null,
				domainResultCreationState
		);

	}
}
