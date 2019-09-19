/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.sql.SqlExpressionResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.CompositeTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.internal.domain.composite.CompositeFetch;
import org.hibernate.sql.results.internal.domain.composite.CompositeResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.Fetchable;

/**
 * @author Steve Ebersole
 */
public class EmbeddedAttributeMapping
		extends AbstractSingularAttributeMapping
		implements EmbeddableValuedModelPart, Fetchable {
	private final String tableExpression;
	private final String[] attrColumnNames;

	public EmbeddedAttributeMapping(
			String name,
			int stateArrayPosition,
			String tableExpression,
			String[] attrColumnNames,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			EmbeddableMappingType valueMapping,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				attributeMetadataAccess,
				mappedFetchStrategy,
				valueMapping,
				declaringType,
				propertyAccess
		);
		this.tableExpression = tableExpression;
		this.attrColumnNames = attrColumnNames;
	}

	@Override
	public EmbeddableMappingType getMappedTypeDescriptor() {
		return (EmbeddableMappingType) super.getMappedTypeDescriptor();
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return getMappedTypeDescriptor();
	}

	@Override
	public SingularAttributeMapping getParentInjectionAttributeMapping() {
		// todo (6.0) : implement
		return null;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public List<String> getMappedColumnExpressions() {
		return Arrays.asList( attrColumnNames );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new CompositeResult<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}


	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new CompositeFetch(
				fetchParent.getNavigablePath().append( getFetchableName() ),
				this,
				fetchParent,
				fetchTiming,
				creationState
		);
	}

	@Override
	public Expression toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final List<ColumnReference> columnReferences = new ArrayList<>();
		final TableReference tableReference = tableGroup.resolveTableReference( getContainingTableExpression() );

		getEmbeddableTypeDescriptor().visitJdbcTypes(
				new Consumer<JdbcMapping>() {
					private int index = 0;

					@Override
					public void accept(JdbcMapping jdbcMapping) {
						final String attrColumnExpr = attrColumnNames[ index++ ];

						final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
								SqlExpressionResolver.createColumnReferenceKey(
										getContainingTableExpression(),
										attrColumnExpr
								),
								sqlAstProcessingState -> tableGroup.resolveColumnReference(
										getContainingTableExpression(),
										attrColumnExpr,
										() -> new ColumnReference(
												attrColumnExpr,
												tableReference.getIdentificationVariable(),
												jdbcMapping,
												sqlAstCreationState.getCreationContext().getSessionFactory()
										)
								)
						);

						columnReferences.add( (ColumnReference) columnReference );
					}
				},
				clause,
				sqlAstCreationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);

		return new SqlTuple( columnReferences, this );
	}

	@Override
	public ModelPart findSubPart(
			String name,
			EntityMappingType treatTargetType) {
		return getMappedTypeDescriptor().findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		getMappedTypeDescriptor().visitSubParts( consumer, treatTargetType );
	}

	@Override
	public TableGroup prepareAsLhs(
			NavigablePath navigablePath,
			SqlAstCreationState creationState) {
		final NavigablePath lhsPath = navigablePath.getParent();

		// NOTE : `lhsPath` refers to this mapping...

		return creationState.getFromClauseAccess().resolveTableGroup(
				lhsPath,
				lnp -> {
					// there is not yet a TableGroup associated with this path - create one...

					final TableGroup lhsLhsTableGroup = SqmMappingModelHelper.resolveLhs( lhsPath, creationState );

					final CompositeTableGroup compositeTableGroup = new CompositeTableGroup(
							lnp,
							this,
							lhsLhsTableGroup
					);

					lhsLhsTableGroup.addTableGroupJoin( new TableGroupJoin( lnp, JoinType.INNER, compositeTableGroup, null ) );

					return compositeTableGroup;
				}
		);
	}
}
