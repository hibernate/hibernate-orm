/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.SortOrder;
import org.hibernate.grammars.ordering.OrderingParser;
import org.hibernate.grammars.ordering.OrderingParserBaseVisitor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.persister.collection.CollectionPersister;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ParseTreeVisitor extends OrderingParserBaseVisitor {
	private static final Logger log = Logger.getLogger( ParseTreeVisitor.class );

	private final PathConsumer pathConsumer;
	private final TranslationContext translationContext;

	private List<SortSpecification> specifications;

	public ParseTreeVisitor(
			PluralAttributeMapping pluralAttributeMapping,
			TranslationContext translationContext) {
		this.pathConsumer = new PathConsumer( pluralAttributeMapping, translationContext );
		this.translationContext = translationContext;
	}

	@Override
	public List<SortSpecification> visitOrderByFragment(OrderingParser.OrderByFragmentContext parsedFragment) {
		final List<OrderingParser.SortSpecificationContext> parsedSortSpecifications = parsedFragment.sortSpecification();
		Objects.requireNonNull( parsedSortSpecifications );

		this.specifications = new ArrayList<>( parsedSortSpecifications.size() );

		for ( OrderingParser.SortSpecificationContext parsedSortSpecification : parsedSortSpecifications ) {
			visitSortSpecification( parsedSortSpecification );
		}

		Objects.requireNonNull( specifications );
		return specifications;
	}

	@Override
	public SortSpecification visitSortSpecification(OrderingParser.SortSpecificationContext parsedSpec) {
		assert parsedSpec != null;
		assert parsedSpec.expression() != null;

		final SortSpecification result = new SortSpecification( visitExpression( parsedSpec.expression() ) );

		if ( parsedSpec.collationSpecification() != null ) {
			result.setCollation( parsedSpec.collationSpecification().identifier().getText() );
		}

		if ( parsedSpec.direction() != null ) {
			if ( parsedSpec.direction().ASC() != null ) {
				result.setSortOrder( SortOrder.ASCENDING );
			}
		}

		// todo (6.0) : null-precedence (see grammar notes)

		return result;
	}

	@Override
	public SortExpression visitExpression(OrderingParser.ExpressionContext ctx) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public String visitCollationSpecification(OrderingParser.CollationSpecificationContext ctx) {
		throw new IllegalStateException( "Unexpected call to #visitCollationSpecification" );
	}
}
