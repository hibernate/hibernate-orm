/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.Statement;

/**
 * The standard translator for SQM to SQL ASTs.
 *
 * @author Christian Beikov
 */
public class StandardSqmTranslator<T extends Statement> extends BaseSqmToSqlAstConverter<T> {

	public StandardSqmTranslator(
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
}
