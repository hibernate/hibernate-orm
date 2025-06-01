/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.query.SortDirection;
import org.hibernate.grammars.ordering.OrderingParser;
import org.hibernate.grammars.ordering.OrderingParserBaseVisitor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import static java.util.Collections.singletonList;
import static org.hibernate.internal.util.QuotingHelper.unquoteIdentifier;

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
	public List<OrderingSpecification> visitOrderByFragment(OrderingParser.OrderByFragmentContext ctx) {
		final int size = ctx.getChildCount();
		// Shift 1 bit instead of division by 2
		final int specificationCount = ( size + 1 ) >> 1;
		if ( specificationCount == 1 ) {
			return singletonList( visitSortSpecification( (OrderingParser.SortSpecificationContext) ctx.getChild( 0 ) ) );
		}
		else {
			final List<OrderingSpecification> specifications = new ArrayList<>( specificationCount );
			for ( int i = 0; i < size; i += 2 ) {
				specifications.add( visitSortSpecification( (OrderingParser.SortSpecificationContext) ctx.getChild( i ) ) );
			}
			return specifications;
		}
	}

	@Override
	public OrderingSpecification visitSortSpecification(OrderingParser.SortSpecificationContext parsedSpec) {
		assert parsedSpec != null;
		assert parsedSpec.expression() != null;

		final OrderingExpression orderingExpression = (OrderingExpression) parsedSpec.getChild( 0 ).accept( this );
		if ( translationContext.getJpaCompliance().isJpaOrderByMappingComplianceEnabled() ) {
			if ( orderingExpression instanceof DomainPath ) {
				// nothing to do
			}
			else {
				throw new OrderByComplianceViolation(
						String.format(
								Locale.ROOT,
								"@OrderBy expression (%s) is not a domain-model reference, which violates the Jakarta Persistence specification - %s",
								parsedSpec.expression().getText(),
								orderingExpression.toDescriptiveText()
						)
				);
			}
		}

		final OrderingSpecification result = new OrderingSpecification( orderingExpression, parsedSpec.getChild( 0 ).getText() );
		int i = 1;

		if ( parsedSpec.getChildCount() > i ) {
			final ParseTree parseTree = parsedSpec.getChild( i );
			if ( parseTree instanceof OrderingParser.CollationSpecificationContext ) {
				result.setCollation( (String) parseTree.getChild( 1 ).getChild( 0 ).accept( this ) );
				i++;
			}
		}
		if ( parsedSpec.getChildCount() > i ) {
			final ParseTree parseTree = parsedSpec.getChild( i );
			if ( parseTree instanceof OrderingParser.DirectionContext directionCtx ) {
				if ( ( (TerminalNode) directionCtx.getChild( 0 ) ).getSymbol().getType() == OrderingParser.ASC ) {
					result.setSortOrder( SortDirection.ASCENDING );
				}
				else {
					result.setSortOrder( SortDirection.DESCENDING );
				}
				i++;
			}
		}
		if ( parsedSpec.getChildCount() > i ) {
			final ParseTree parseTree = parsedSpec.getChild( i );
			if ( parseTree instanceof OrderingParser.NullsPrecedenceContext nullsCtx ) {
				if ( ( (TerminalNode) nullsCtx.getChild( 1 ) ).getSymbol().getType() == OrderingParser.FIRST ) {
					result.setNullPrecedence( Nulls.FIRST );
				}
				else {
					result.setNullPrecedence( Nulls.LAST );
				}
			}
		}

		return result;
	}

	@Override
	public OrderingExpression visitFunctionExpression(OrderingParser.FunctionExpressionContext ctx) {
		return visitFunction( (OrderingParser.FunctionContext) ctx.getChild( 0 ) );
	}

	@Override
	public OrderingExpression visitIdentifierExpression(OrderingParser.IdentifierExpressionContext ctx) {
		return visitIdentifier( (OrderingParser.IdentifierContext) ctx.getChild( 0 ) );
	}

	@Override
	public OrderingExpression visitDotIdentifierExpression(OrderingParser.DotIdentifierExpressionContext ctx) {
		return visitDotIdentifier( (OrderingParser.DotIdentifierContext) ctx.getChild( 0 ) );
	}

	@Override
	public OrderingExpression visitDotIdentifier(OrderingParser.DotIdentifierContext ctx) {
		final int size = ctx.getChildCount();
		final int end = size - 1;
		// For nested paths, which must be on the domain model, we don't care about the possibly quoted identifier,
		// so we just pass the unquoted one
		String partName = (String) ctx.getChild( 0 ).getChild( 0 ).accept( this );
		pathConsumer.consumeIdentifier(
				partName,
				partName,
				true,
				false
		);

		for ( int i = 2; i < end; i += 2 ) {
			partName = (String) ctx.getChild( i ).getChild( 0 ).accept( this );
			pathConsumer.consumeIdentifier(
					partName,
					partName,
					false,
					false
			);
		}
		partName = (String) ctx.getChild( end ).getChild( 0 ).accept( this );
		pathConsumer.consumeIdentifier(
				partName,
				partName,
				false,
				true
		);

		return (OrderingExpression) pathConsumer.getConsumedPart();
	}

	@Override
	public OrderingExpression visitIdentifier(OrderingParser.IdentifierContext ctx) {
		final String unquotedIdentifier = (String) ctx.getChild( 0 ).accept( this );
		final SqmFunctionDescriptor descriptor = translationContext.getFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionDescriptor( unquotedIdentifier );
		// If there is no function with this name, it always requires parenthesis or if this is a quoted identifiers
		// then we interpret this as a path part instead of as function
		final String identifier = ctx.getChild( 0 ).getText();
		if ( descriptor == null || descriptor.alwaysIncludesParentheses() || !unquotedIdentifier.equals( identifier ) ) {
			pathConsumer.consumeIdentifier( unquotedIdentifier, identifier, true, true );
			return (OrderingExpression) pathConsumer.getConsumedPart();
		}
		return new SelfRenderingOrderingExpression( unquotedIdentifier );
	}

	@Override
	public FunctionExpression visitFunction(OrderingParser.FunctionContext ctx) {
		final ParseTree functionCtx = ctx.getChild( 0 );
		final OrderingParser.FunctionArgumentsContext argumentsCtx = (OrderingParser.FunctionArgumentsContext) functionCtx.getChild( 1 );
		final int size = argumentsCtx.getChildCount();
		// Shift 1 bit instead of division by 2
		final int expressionsCount = ( ( size - 1 ) >> 1 );
		final FunctionExpression function = new FunctionExpression(
				functionCtx.getChild( 0 ).getText(),
				expressionsCount
		);

		for ( int i = 1; i < size; i += 2 ) {
			function.addArgument( (OrderingExpression) argumentsCtx.getChild( i ).accept( this ) );
		}

		return function;
	}

	@Override
	public OrderingExpression visitFunctionArgument(OrderingParser.FunctionArgumentContext ctx) {
		return (OrderingExpression) ctx.getChild( 0 ).accept( this );
	}

	@Override
	public OrderingExpression visitLiteral(OrderingParser.LiteralContext ctx) {
		return new SelfRenderingOrderingExpression( ctx.getText() );
	}

	@Override
	public String visitCollationSpecification(OrderingParser.CollationSpecificationContext ctx) {
		throw new IllegalStateException( "Unexpected call to #visitCollationSpecification" );
	}

	@Override
	public Object visitTerminal(TerminalNode node) {
		return switch ( node.getSymbol().getType() ) {
			case OrderingParser.EOF -> null;
			case OrderingParser.IDENTIFIER -> node.getText();
			case OrderingParser.QUOTED_IDENTIFIER -> unquoteIdentifier( node.getText() );
			default -> throw new ParsingException( "Unexpected terminal node [" + node.getText() + "]" );
		};
	}
}
