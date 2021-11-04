/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.function;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.sql.FromClauseIndex;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Envers specific FunctionContributor
 *
 * @author Christian Beikov
 */
public class OrderByFragmentFunction extends AbstractSqmFunctionDescriptor {

	public static final String FUNCTION_NAME = "_order_by_frag";
	public static final OrderByFragmentFunction INSTANCE = new OrderByFragmentFunction();

	public OrderByFragmentFunction() {
		super(
				FUNCTION_NAME,
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 1 )
		);
	}
	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return new SelfRenderingSqmFunction<T>(
				this,
				null,
				arguments,
				impliedResultType,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		) {

			@Override
			public SelfRenderingFunctionSqlAstExpression convertToSqlAst(SqmToSqlAstConverter walker) {
				final AllowableFunctionReturnType<?> resultType = resolveResultType(
						walker.getCreationContext().getDomainModel().getTypeConfiguration()
				);
				final String sqmAlias = ( (SqmLiteral<String>) getArguments().get( 0 ) ).getLiteralValue();
				final String attributeRole = ( (SqmLiteral<String>) getArguments().get( 1 ) ).getLiteralValue();
				final TableGroup tableGroup = ( (FromClauseIndex) walker.getFromClauseAccess() ).findTableGroup(
						sqmAlias
				);
				final QueryableCollection collectionDescriptor = (QueryableCollection) walker.getCreationContext()
						.getDomainModel()
						.findCollectionDescriptor( attributeRole );
				final PluralAttributeMapping pluralAttribute = collectionDescriptor.getAttributeMapping();
				final QuerySpec queryPart = (QuerySpec) ( (SqlAstQueryPartProcessingState) walker.getCurrentProcessingState() ).getInflightQueryPart();
				final OrderByFragment fragment;
				if ( pluralAttribute.getOrderByFragment() != null ) {
					fragment = pluralAttribute.getOrderByFragment();
				}
				else {
					fragment = pluralAttribute.getManyToManyOrderByFragment();
				}
				final String targetTableExpression;
				if ( collectionDescriptor.getElementPersister() == null ) {
					targetTableExpression = collectionDescriptor.getTableName();
				}
				else {
					targetTableExpression = ( (Joinable) collectionDescriptor.getElementPersister() ).getTableName();
				}
				// We apply the fragment here and return null to signal that this is a no-op
				fragment.apply(
						queryPart,
						new AuditingTableGroup( tableGroup, targetTableExpression ),
						walker
				);
				return null;
			}
		};
	}

	private static class AuditingTableGroup implements TableGroup {

		private final TableGroup delegate;
		private final String auditTableExpression;
		private final String normalTableExpression;

		public AuditingTableGroup(TableGroup delegate, String normalTableExpression) {
			this.delegate = delegate;
			this.auditTableExpression = delegate.getPrimaryTableReference().getTableExpression();
			this.normalTableExpression = normalTableExpression;
		}

		@Override
		public ModelPart getExpressionType() {
			return delegate.getExpressionType();
		}

		@Override
		public TableReference resolveTableReference(
				NavigablePath navigablePath,
				String tableExpression,
				boolean allowFkOptimization) {
			if ( tableExpression.equals( normalTableExpression ) ) {
				tableExpression = auditTableExpression;
			}
			return delegate.resolveTableReference( navigablePath, tableExpression, allowFkOptimization );
		}

		@Override
		public TableReference getTableReference(
				NavigablePath navigablePath,
				String tableExpression,
				boolean allowFkOptimization,
				boolean resolve) {
			if ( tableExpression.equals( normalTableExpression ) ) {
				tableExpression = auditTableExpression;
			}
			return delegate.getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
		}

		@Override
		public NavigablePath getNavigablePath() {
			return delegate.getNavigablePath();
		}

		@Override
		public String getGroupAlias() {
			return delegate.getGroupAlias();
		}

		@Override
		public ModelPartContainer getModelPart() {
			return delegate.getModelPart();
		}

		@Override
		public String getSourceAlias() {
			return delegate.getSourceAlias();
		}

		@Override
		public List<TableGroupJoin> getTableGroupJoins() {
			return delegate.getTableGroupJoins();
		}

		@Override
		public List<TableGroupJoin> getNestedTableGroupJoins() {
			return delegate.getNestedTableGroupJoins();
		}

		@Override
		public boolean canUseInnerJoins() {
			return delegate.canUseInnerJoins();
		}

		@Override
		public void addTableGroupJoin(TableGroupJoin join) {
			delegate.addTableGroupJoin( join );
		}

		@Override
		public void addNestedTableGroupJoin(TableGroupJoin join) {
			delegate.addNestedTableGroupJoin( join );
		}

		@Override
		public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
			delegate.visitTableGroupJoins( consumer );
		}

		@Override
		public void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer) {
			delegate.visitNestedTableGroupJoins( consumer );
		}

		@Override
		public void applyAffectedTableNames(Consumer<String> nameCollector) {
			delegate.applyAffectedTableNames( nameCollector );
		}

		@Override
		public TableReference getPrimaryTableReference() {
			return delegate.getPrimaryTableReference();
		}

		@Override
		public List<TableReferenceJoin> getTableReferenceJoins() {
			return delegate.getTableReferenceJoins();
		}
	}
}
