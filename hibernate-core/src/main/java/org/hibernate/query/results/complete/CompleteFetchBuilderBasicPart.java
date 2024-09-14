/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.results.complete;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.BasicValuedFetchBuilder;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.MissingSqlSelectionException;
import org.hibernate.query.results.PositionalSelectionsNotAllowedException;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.query.results.ResultsHelper.jdbcPositionToValuesArrayPosition;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class CompleteFetchBuilderBasicPart implements CompleteFetchBuilder, BasicValuedFetchBuilder, ModelPartReferenceBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart referencedModelPart;
	private final String selectionAlias;

	public CompleteFetchBuilderBasicPart(
			NavigablePath navigablePath,
			BasicValuedModelPart referencedModelPart,
			String selectionAlias) {
		this.navigablePath = navigablePath;
		this.referencedModelPart = referencedModelPart;
		this.selectionAlias = selectionAlias;
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public BasicValuedModelPart getReferencedPart() {
		return referencedModelPart;
	}

	@Override
	public List<String> getColumnAliases() {
		return Collections.singletonList( selectionAlias );
	}

	@Override
	public BasicFetch<?> buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );

		final String mappedTable = referencedModelPart.getContainingTableExpression();

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( parent.getNavigablePath() );
		final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, referencedModelPart, mappedTable );

		final String selectedAlias;
		final int jdbcPosition;

		if ( selectionAlias != null ) {
			try {
				jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( selectionAlias );
			}
			catch (Exception e) {
				throw new MissingSqlSelectionException(
						"ResultSet mapping specified selected-alias `" + selectionAlias
								+ "` which was not part of the ResultSet",
						e
				);
			}
			selectedAlias = selectionAlias;
		}
		else {
			if ( ! creationStateImpl.arePositionalSelectionsAllowed() ) {
				throw new PositionalSelectionsNotAllowedException(
						"Positional SQL selection resolution not allowed"
				);
			}
			jdbcPosition = creationStateImpl.getNumberOfProcessedSelections() + 1;
			selectedAlias = jdbcResultsMetadata.resolveColumnName( jdbcPosition );
		}

		final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );

		final JdbcMapping jdbcMapping;
		if ( referencedModelPart instanceof DiscriminatorMapping ) {
			jdbcMapping = ( (DiscriminatorMapping) referencedModelPart ).getUnderlyingJdbcMapping();
		}
		else {
			jdbcMapping = referencedModelPart.getJdbcMapping();
		}

		// we just care about the registration here.  The ModelPart will find it later
		creationStateImpl.resolveSqlExpression(
				createColumnReferenceKey( tableReference, referencedModelPart.getSelectablePath(), jdbcMapping ),
				processingState -> new ResultSetMappingSqlSelection( valuesArrayPosition, referencedModelPart )
		);

		return (BasicFetch<?>) parent.generateFetchableFetch(
				referencedModelPart,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				selectedAlias,
				domainResultCreationState
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final CompleteFetchBuilderBasicPart that = (CompleteFetchBuilderBasicPart) o;
		return navigablePath.equals( that.navigablePath )
				&& referencedModelPart.equals( that.referencedModelPart )
				&& Objects.equals( selectionAlias, that.selectionAlias );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + referencedModelPart.hashCode();
		result = 31 * result + ( selectionAlias != null ? selectionAlias.hashCode() : 0 );
		return result;
	}
}
