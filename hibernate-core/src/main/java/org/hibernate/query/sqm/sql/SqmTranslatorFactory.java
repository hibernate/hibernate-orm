/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * Factory for various {@link SqmTranslator}s
 *
 * @author Steve Ebersole
 */
public interface SqmTranslatorFactory {
	SqmTranslator<SelectStatement> createSelectTranslator(
			SqmSelectStatement<?> sqmSelectStatement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext,
			boolean deduplicateSelectionItems);

	SqmTranslator<? extends MutationStatement> createMutationTranslator(
			SqmDmlStatement<?> sqmDmlStatement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext);
}
