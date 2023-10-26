/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.query.ReturnableType;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.sql.FromClauseIndex;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.DelegatingTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;

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
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				null
		);
	}
	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new OrderByFragmentSelfRenderingSqmFunction<>( this, arguments, impliedResultType, queryEngine );
	}

	private static class AuditingTableGroup extends DelegatingTableGroup {

		private final TableGroup delegate;
		private final String auditTableExpression;
		private final String normalTableExpression;

		public AuditingTableGroup(TableGroup delegate, String normalTableExpression) {
			this.delegate = delegate;
			this.auditTableExpression = ( (NamedTableReference) delegate.getPrimaryTableReference() ).getTableExpression();
			this.normalTableExpression = normalTableExpression;
		}

		@Override
		protected TableGroup getTableGroup() {
			return delegate;
		}

		@Override
		public TableReference resolveTableReference(
				NavigablePath navigablePath,
				String tableExpression) {
			if ( tableExpression.equals( normalTableExpression ) ) {
				tableExpression = auditTableExpression;
			}
			return super.resolveTableReference( navigablePath, tableExpression );
		}

		@Override
		public TableReference resolveTableReference(
				NavigablePath navigablePath,
				ValuedModelPart modelPart,
				String tableExpression) {
			if ( tableExpression.equals( normalTableExpression ) ) {
				return resolveTableReference( navigablePath, modelPart, auditTableExpression );
			}
			return super.resolveTableReference( navigablePath, modelPart, tableExpression );
		}

		@Override
		public TableReference getTableReference(
				NavigablePath navigablePath,
				String tableExpression,
				boolean resolve) {
			if ( tableExpression.equals( normalTableExpression ) ) {
				tableExpression = auditTableExpression;
			}
			return super.getTableReference( navigablePath, tableExpression, resolve );
		}

		@Override
		public TableReference getTableReference(
				NavigablePath navigablePath,
				ValuedModelPart modelPart,
				String tableExpression,
				boolean resolve) {
			if ( tableExpression.equals( normalTableExpression ) ) {
				return getTableReference( navigablePath, modelPart, auditTableExpression, resolve );
			}
			return super.getTableReference( navigablePath, modelPart, tableExpression, resolve );
		}
	}

	private static class OrderByFragmentSelfRenderingSqmFunction<T> extends SelfRenderingSqmFunction<T> {

		public OrderByFragmentSelfRenderingSqmFunction(
				OrderByFragmentFunction orderByFragmentFunction,
				List<? extends SqmTypedNode<?>> arguments,
				ReturnableType<T> impliedResultType,
				QueryEngine queryEngine) {
			super(
					orderByFragmentFunction,
					null,
					arguments,
					impliedResultType,
					orderByFragmentFunction.getArgumentsValidator(),
					orderByFragmentFunction.getReturnTypeResolver(),
					queryEngine.getCriteriaBuilder(),
					orderByFragmentFunction.getName()
			);
		}

		private OrderByFragmentSelfRenderingSqmFunction(
				SqmFunctionDescriptor descriptor,
				FunctionRenderer renderer,
				List<? extends SqmTypedNode<?>> arguments,
				ReturnableType<T> impliedResultType,
				ArgumentsValidator argumentsValidator,
				FunctionReturnTypeResolver returnTypeResolver,
				NodeBuilder nodeBuilder,
				String name) {
			super(
					descriptor,
					renderer,
					arguments,
					impliedResultType,
					argumentsValidator,
					returnTypeResolver,
					nodeBuilder,
					name
			);
		}

		@Override
		public OrderByFragmentSelfRenderingSqmFunction<T> copy(SqmCopyContext context) {
			final OrderByFragmentSelfRenderingSqmFunction<T> existing = context.getCopy( this );
			if ( existing != null ) {
				return existing;
			}
			final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
			for ( SqmTypedNode<?> argument : getArguments() ) {
				arguments.add( argument.copy( context ) );
			}
			return context.registerCopy(
					this,
					new OrderByFragmentSelfRenderingSqmFunction<>(
							getFunctionDescriptor(),
							getFunctionRenderer(),
							arguments,
							getImpliedResultType(),
							getArgumentsValidator(),
							getReturnTypeResolver(),
							nodeBuilder(),
							getFunctionName()
					)
			);
		}

		@Override
		public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
			final String sqmAlias = ( (SqmLiteral<String>) getArguments().get( 0 ) ).getLiteralValue();
			final String attributeRole = ( (SqmLiteral<String>) getArguments().get( 1 ) ).getLiteralValue();
			final TableGroup tableGroup = ( (FromClauseIndex) walker.getFromClauseAccess() ).findTableGroup(
					sqmAlias
			);
			final QueryableCollection collectionDescriptor = (QueryableCollection) walker.getCreationContext()
					.getSessionFactory()
						.getRuntimeMetamodels()
						.getMappingMetamodel()
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
	}
}
