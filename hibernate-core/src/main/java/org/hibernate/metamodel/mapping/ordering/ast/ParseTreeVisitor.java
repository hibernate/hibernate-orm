/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.SortOrder;
import org.hibernate.grammars.ordering.OrderingParser;
import org.hibernate.grammars.ordering.OrderingParser.ExpressionContext;
import org.hibernate.grammars.ordering.OrderingParserBaseVisitor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;

import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * @author Steve Ebersole
 */
public class ParseTreeVisitor extends OrderingParserBaseVisitor<Object> {
	private final PathConsumer pathConsumer;
	private final TranslationContext translationContext;

	public ParseTreeVisitor(
			PluralAttributeMapping pluralAttributeMapping,
			TranslationContext translationContext) {
		this.pathConsumer = new PathConsumer( pluralAttributeMapping, translationContext );
		this.translationContext = translationContext;
	}

	@Override
	public List<OrderingSpecification> visitOrderByFragment(OrderingParser.OrderByFragmentContext parsedFragment) {
		final List<OrderingParser.SortSpecificationContext> parsedSortSpecifications = parsedFragment.sortSpecification();
		assert parsedSortSpecifications != null;

		if ( parsedSortSpecifications.size() == 1 ) {
			return Collections.singletonList( visitSortSpecification( parsedSortSpecifications.get( 0 ) ) );
		}

		final List<OrderingSpecification> specifications = new ArrayList<>( parsedSortSpecifications.size() );

		for ( OrderingParser.SortSpecificationContext parsedSortSpecification : parsedSortSpecifications ) {
			specifications.add( visitSortSpecification( parsedSortSpecification ) );
		}

		return specifications;
	}

	@Override
	public OrderingSpecification visitSortSpecification(OrderingParser.SortSpecificationContext parsedSpec) {
		assert parsedSpec != null;
		assert parsedSpec.expression() != null;

		final OrderingExpression orderingExpression = visitExpression( parsedSpec.expression() );
		if ( translationContext.getJpaCompliance().isJpaOrderByMappingComplianceEnabled() ) {
			if ( orderingExpression instanceof DomainPath ) {
				// nothing to do
			}
			else {
				throw new OrderByComplianceViolation(
						"`@OrderBy` expression (" + parsedSpec.expression().getText()
								+ ") resolved to `" + orderingExpression
								+ "` which is not a domain-model reference which violates the JPA specification"
				);
			}
		}

		final OrderingSpecification result = new OrderingSpecification( orderingExpression );

		if ( parsedSpec.collationSpecification() != null ) {
			result.setCollation( parsedSpec.collationSpecification().identifier().getText() );
		}

		if ( parsedSpec.direction() != null && parsedSpec.direction().DESC() != null ) {
			result.setSortOrder( SortOrder.DESCENDING );
		}
		else {
			result.setSortOrder( SortOrder.ASCENDING );
		}

		// todo (6.0) : null-precedence (see grammar notes)

		return result;
	}

	@Override
	public OrderingExpression visitExpression(ExpressionContext ctx) {
		if ( ctx.function() != null ) {
			return visitFunction( ctx.function() );
		}

		if ( ctx.identifier() != null ) {
			pathConsumer.consumeIdentifier( ctx.identifier().getText(), true, true );
			return (OrderingExpression) pathConsumer.getConsumedPart();
		}

		assert ctx.dotIdentifier() != null;
		final int numberOfParts = ctx.dotIdentifier().IDENTIFIER().size();
		boolean firstPass = true;

		for ( int i = 0; i < numberOfParts; i++ ) {
			final TerminalNode partNode = ctx.dotIdentifier().IDENTIFIER().get( i );
			pathConsumer.consumeIdentifier(
					partNode.getText(),
					firstPass,
					true
			);
			firstPass = false;
		}

		return (OrderingExpression) pathConsumer.getConsumedPart();
	}

	@Override
	public FunctionExpression visitFunction(OrderingParser.FunctionContext ctx) {
		if ( ctx.simpleFunction() != null ) {
			final FunctionExpression function = new FunctionExpression(
					ctx.simpleFunction().identifier().getText(),
					ctx.simpleFunction().functionArguments().expression().size()
			);

			for ( int i = 0; i < ctx.simpleFunction().functionArguments().expression().size(); i++ ) {
				final ExpressionContext arg = ctx.simpleFunction().functionArguments().expression( i );
				function.addArgument( visitExpression( arg ) );
			}

			return function;
		}

		assert ctx.packagedFunction() != null;

		final FunctionExpression function = new FunctionExpression(
				ctx.packagedFunction().dotIdentifier().getText(),
				ctx.packagedFunction().functionArguments().expression().size()
		);

		for ( int i = 0; i < ctx.packagedFunction().functionArguments().expression().size(); i++ ) {
			final ExpressionContext arg = ctx.packagedFunction().functionArguments().expression( i );
			function.addArgument( visitExpression( arg ) );
		}

		return function;
	}

	@Override
	public String visitCollationSpecification(OrderingParser.CollationSpecificationContext ctx) {
		throw new IllegalStateException( "Unexpected call to #visitCollationSpecification" );
	}
}
