/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.internal.StandardSqmTranslator;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 * Standard implementation of the SqmTranslatorFactory
 *
 * @author Steve Ebersole
 */
public class StandardSqmTranslatorFactory implements SqmTranslatorFactory {

	@Override
	public SqmTranslator<SelectStatement> createSelectTranslator(
			SqmSelectStatement<?> sqmSelectStatement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext,
			boolean deduplicateSelectionItems) {
		return new StandardSqmTranslator<>(
				sqmSelectStatement,
				queryOptions,
				domainParameterXref,
				domainParameterBindings,
				loadQueryInfluencers,
				creationContext,
				deduplicateSelectionItems
		);
	}

	@Override
	public SqmTranslator<? extends MutationStatement> createMutationTranslator(
			SqmDmlStatement<?> sqmDeleteStatement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext) {
		return new StandardSqmTranslator<>(
				sqmDeleteStatement,
				queryOptions,
				domainParameterXref,
				domainParameterBindings,
				loadQueryInfluencers,
				creationContext,
				false
		);
	}

	@Override
	public SqmTranslator<DeleteStatement> createSimpleDeleteTranslator(
			SqmDeleteStatement<?> sqmDeleteStatement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext) {
		return new StandardSqmTranslator<>(
				sqmDeleteStatement,
				queryOptions,
				domainParameterXref,
				domainParameterBindings,
				loadQueryInfluencers,
				creationContext,
				false
		);
	}

	@Override
	public SqmTranslator<InsertStatement> createInsertTranslator(
			SqmInsertStatement<?> sqmInsertStatement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext) {
		return new StandardSqmTranslator<>(
				sqmInsertStatement,
				queryOptions,
				domainParameterXref,
				domainParameterBindings,
				loadQueryInfluencers,
				creationContext,
				false
		);
	}

	@Override
	public SqmTranslator<UpdateStatement> createSimpleUpdateTranslator(
			SqmUpdateStatement<?> sqmUpdateStatement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext) {
		return new StandardSqmTranslator<>(
				sqmUpdateStatement,
				queryOptions,
				domainParameterXref,
				domainParameterBindings,
				loadQueryInfluencers,
				creationContext,
				false
		);
	}
}
