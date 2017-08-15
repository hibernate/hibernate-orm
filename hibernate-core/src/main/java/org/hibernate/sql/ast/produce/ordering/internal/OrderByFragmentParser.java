/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.internal.hql.SemanticQueryBuilder;
import org.hibernate.query.sqm.produce.internal.hql.grammar.HqlParser;
import org.hibernate.query.sqm.produce.internal.hql.navigable.NavigableBindingResolver;
import org.hibernate.query.sqm.produce.internal.hql.navigable.PathHelper;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;

import org.jboss.logging.Logger;

/**
 * Specialized HQL parse tree visitation for handling {@link javax.persistence.OrderBy} which allows for a
 * "valid JPQL order-by fragment", as opposed to {@link org.hibernate.annotations.OrderBy} which
 * expects raw SQL.
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentParser extends SemanticQueryBuilder {
	private static final Logger log = Logger.getLogger( OrderByFragmentParser.class.getName() );

	public static SqmOrderByClause convertOrderByFragmentParseTree(
			TranslationContext translationContext,
			PersistentCollectionDescriptor collectionDescriptor,
			HqlParser.OrderByClauseContext orderByClauseContext) {
		return new OrderByFragmentParser( translationContext, collectionDescriptor )
				.visitOrderByClause( orderByClauseContext );
	}

	private final SqmFrom sqmFromBase;

	private final NavigableBindingResolver navigableBindingResolver;
	private final List<SqmSortSpecification> sqmSortSpecifications = new ArrayList<>();


	public OrderByFragmentParser(TranslationContext translationContext, PersistentCollectionDescriptor collectionDescriptor) {
		super( new ParsingContext( translationContext.getSessionFactory() ) );
		this.sqmFromBase = new SqmFromImpl( getParsingContext(), collectionDescriptor );

		this.navigableBindingResolver = new PathResolverImpl( getParsingContext(), sqmFromBase );
		primeStack( getPathResolverStack(), navigableBindingResolver );

		primeStack( getParameterDeclarationContextStack(), () -> false );
	}

	@Override
	public SqmSortSpecification visitSortSpecification(HqlParser.SortSpecificationContext ctx) {
		final SqmSortSpecification spec = super.visitSortSpecification( ctx );
		sqmSortSpecifications.add( spec );
		return spec;
	}

	@Override
	public Object visitPathExpression(HqlParser.PathExpressionContext ctx) {
		final String pathText = ctx.path().getText();
		final String[] pathSplits = PathHelper.split( pathText );
		final SqmNavigableReference resolvedNavigableReference = navigableBindingResolver.resolvePath( pathSplits );

		if ( resolvedNavigableReference != null ) {
			return resolvedNavigableReference;
		}

		if ( pathSplits.length > 1 ) {
			throw new SemanticException(
					String.format(
							Locale.ROOT,
							"Encountered compound path expression [%s] (relative to [%s]) " +
									"which did not resolve to navigable reference; " +
									"and due to being compound, the path cannot be column name",
							pathText,
							sqmFromBase
					)
			);
		}

		// otherwise assume we have a column name
		return new SqmColumnReference( sqmFromBase );
	}

	public List<SqmSortSpecification> getSqmSortSpecifications() {
		return sqmSortSpecifications;
	}
}
