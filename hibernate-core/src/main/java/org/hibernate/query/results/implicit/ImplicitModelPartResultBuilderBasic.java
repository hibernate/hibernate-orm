/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class ImplicitModelPartResultBuilderBasic
		implements ImplicitModelPartResultBuilder, ResultBuilderBasicValued {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart modelPart;

	public ImplicitModelPartResultBuilderBasic(NavigablePath navigablePath, BasicValuedModelPart modelPart) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
	}

	@Override
	public Class<?> getJavaType() {
		return modelPart.getExpressableJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );

		final TableGroup tableGroup = creationStateImpl
				.getFromClauseAccess()
				.getTableGroup( navigablePath.getParent() );
		return (BasicResult<?>) modelPart.createDomainResult( navigablePath, tableGroup, null, domainResultCreationState );
	}
}
