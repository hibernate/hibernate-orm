/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.function;

import java.util.List;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.query.ReturnableType;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.spi.NavigablePath;
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
import org.hibernate.sql.ast.tree.from.DelegatingTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
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
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return new SelfRenderingSqmFunction<T>(
				this,
				null,
				arguments,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		) {

			@Override
			public SelfRenderingFunctionSqlAstExpression convertToSqlAst(SqmToSqlAstConverter walker) {
				final ReturnableType<?> resultType = resolveResultType(
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
				String tableExpression,
				boolean allowFkOptimization) {
			if ( tableExpression.equals( normalTableExpression ) ) {
				tableExpression = auditTableExpression;
			}
			return super.resolveTableReference( navigablePath, tableExpression, allowFkOptimization );
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
			return super.getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
		}
	}
}
