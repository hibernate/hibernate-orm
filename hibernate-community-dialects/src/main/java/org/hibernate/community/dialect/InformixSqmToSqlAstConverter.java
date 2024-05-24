/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.EntityValuedPathInterpretation;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;

import static java.util.Collections.singletonList;

/**
 * A SQM to SQL AST translator for Informix.
 *
 * @author Christian Beikov
 */
public class InformixSqmToSqlAstConverter<T extends Statement> extends BaseSqmToSqlAstConverter<T> {

	private boolean needsDummyTableGroup;

	public InformixSqmToSqlAstConverter(
			SqmStatement<?> statement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers fetchInfluencers,
			SqlAstCreationContext creationContext,
			boolean deduplicateSelectionItems) {
		super(
				creationContext,
				statement,
				queryOptions,
				fetchInfluencers,
				domainParameterXref,
				domainParameterBindings,
				deduplicateSelectionItems
		);
	}

	@Override
	public QuerySpec visitQuerySpec(SqmQuerySpec<?> sqmQuerySpec) {
		final boolean needsDummy = this.needsDummyTableGroup;
		this.needsDummyTableGroup = false;
		try {
			final QuerySpec querySpec = super.visitQuerySpec( sqmQuerySpec );
			if ( this.needsDummyTableGroup ) {
				querySpec.getFromClause().addRoot(
						new StandardTableGroup(
								true,
								null,
								null,
								null,
								new NamedTableReference( "(select 1)", "dummy_(x)" ),
								null,
								getCreationContext().getSessionFactory()
						)
				);
			}
			return querySpec;
		}
		finally {
			this.needsDummyTableGroup = needsDummy;
		}
	}

	@Override
	protected Expression resolveGroupOrOrderByExpression(SqmExpression<?> groupByClauseExpression) {
		final Expression expression = super.resolveGroupOrOrderByExpression( groupByClauseExpression );
		if ( expression instanceof Literal ) {
			// Note that SqlAstTranslator.renderPartitionItem depends on this
			this.needsDummyTableGroup = true;
		}
		return expression;
	}

	@Override
	public Expression visitPluralAttributeSizeFunction(SqmCollectionSize function) {
		SelectStatement result = (SelectStatement) super.visitPluralAttributeSizeFunction(function);
		if (getDialect().getVersion().isBefore( 11, 70 )) {
			TableGroup tableGroup1 = result.getQuerySpec().getFromClause().getRoots().get( 0 );
			final EntityValuedModelPart tableGroupModelPart = (EntityValuedModelPart) ( (PluralAttributeMapping) tableGroup1.getModelPart() ).getElementDescriptor();
			final NavigablePath navigablePath = tableGroup1.getNavigablePath()
					.append( tableGroupModelPart.getPartName() );
			final EntityMappingType treatedMapping = tableGroupModelPart.getEntityMappingType();
			final ModelPart resultModelPart = treatedMapping.getIdentifierMapping();
			Expression argument = EntityValuedPathInterpretation.from(
					navigablePath,
					tableGroup1,
					resultModelPart,
					tableGroupModelPart,
					treatedMapping,
					this
			);
			final AbstractSqmSelfRenderingFunctionDescriptor functionDescriptor = (AbstractSqmSelfRenderingFunctionDescriptor) getCreationContext()
					.getSessionFactory()
					.getQueryEngine()
					.getSqmFunctionRegistry()
					.findFunctionDescriptor( "count" );
			final BasicType<Integer> integerType = getCreationContext().getMappingMetamodel()
					.getTypeConfiguration()
					.getBasicTypeForJavaType( Integer.class );
			final Expression expression = new SelfRenderingAggregateFunctionSqlAstExpression(
					functionDescriptor.getName(),
					functionDescriptor,
					singletonList( argument ),
					null,
					integerType,
					integerType
			);
			result.getQuerySpec().getSelectClause().getSqlSelections().set( 0, new SqlSelectionImpl( expression ) );
		}
		return result;
	}

}
