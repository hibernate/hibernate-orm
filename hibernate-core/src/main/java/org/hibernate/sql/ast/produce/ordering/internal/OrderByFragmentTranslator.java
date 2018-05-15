/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.sqm.produce.internal.hql.HqlParseTreeBuilder;
import org.hibernate.query.sqm.produce.internal.hql.HqlParseTreePrinter;
import org.hibernate.query.sqm.produce.internal.hql.grammar.HqlParser;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlQueryOptions;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;

import org.jboss.logging.Logger;

/**
 * A translator for order-by mappings, whether specified by hbm.xml files, Hibernate
 * {@link org.hibernate.annotations.OrderBy} annotation or JPA {@link javax.persistence.OrderBy} annotation.
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentTranslator implements SqlAstProducerContext, SqlQueryOptions {
	private static final Logger LOG = Logger.getLogger( OrderByFragmentTranslator.class.getName() );

	/**
	 * Perform the translation of the user-supplied fragment, returning the translation.
	 * <p/>
	 * The important distinction to this split between (1) translating and (2) resolving the
	 * "concrete SQL aliases" is that both happen at different times:
	 *
	 * 	1. The fragment is "translated" as part of bootstrap process, which means passing
	 * 		it through this OrderByFragmentTranslator.  The translation involves adding
	 * 		"placeholder" tokens for injecting the table aliases later in step 2
	 * 2. During runtime, as we need to embed the order-by fragment into larger
	 * 		SQL SELECT queries, those placeholders will be replaced by simple
	 * 		String replacement techniques.
	 *
	 * @param context Context giving access to delegates needed during translation.
	 * @param fragment The user-supplied order-by fragment
	 *
	 * @return The translation.
	 */
	public static OrderByTranslation translate(
			TranslationContext context,
			PersistentCollectionDescriptor collectionDescriptor,
			String fragment) {
		return new OrderByFragmentTranslator( context, collectionDescriptor )
				.translateFragment( fragment );
	}


	private final TranslationContext translationContext;
	private final PersistentCollectionDescriptor collectionDescriptor;

	protected OrderByFragmentTranslator(
			TranslationContext translationContext,
			PersistentCollectionDescriptor collectionDescriptor) {
		this.translationContext = translationContext;
		this.collectionDescriptor = collectionDescriptor;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return translationContext.getSessionFactory();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return LoadQueryInfluencers.NONE;
	}

	@Override
	public Callback getCallback() {
		return afterLoadAction -> {};
	}

	private OrderByTranslation translateFragment(String fragment) {
		final HqlParser parseTree = HqlParseTreeBuilder.INSTANCE.parseHql( fragment );

		HqlParseTreePrinter.logOrderByParseTree( parseTree );

		final SqmOrderByClause sqmOrderByClause = OrderByFragmentParser.convertOrderByFragmentParseTree(
				translationContext,
				collectionDescriptor,
				parseTree.orderByClause()
		);

		final List<SortSpecification> sortSpecifications = OrderByFragmentConverter.convertOrderByFragmentSqmTree(
				this,
				sqmOrderByClause
		);

		return OrderByFragmentRenderer.renderOrderByFragment( translationContext, collectionDescriptor, sortSpecifications );

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlQueryOptions

	@Override
	public Integer getFirstRow() {
		return null;
	}

	@Override
	public Integer getMaxRows() {
		return null;
	}

	@Override
	public LockOptions getLockOptions() {
		return LockOptions.NONE;
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public List<String> getDatabaseHints() {
		return null;
	}
}
