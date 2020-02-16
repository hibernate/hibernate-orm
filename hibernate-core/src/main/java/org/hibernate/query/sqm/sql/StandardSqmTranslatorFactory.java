/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.internal.StandardSqmDeleteTranslator;
import org.hibernate.query.sqm.sql.internal.StandardSqmInsertTranslator;
import org.hibernate.query.sqm.sql.internal.StandardSqmSelectTranslator;
import org.hibernate.query.sqm.sql.internal.StandardSqmUpdateTranslator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;

/**
 * Standard implementation of the SqmTranslatorFactory
 *
 * @author Steve Ebersole
 */
public class StandardSqmTranslatorFactory implements SqmTranslatorFactory {
	@Override
	public SqmSelectTranslator createSelectTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers influencers,
			SqlAstCreationContext creationContext) {
		return new StandardSqmSelectTranslator(
				queryOptions,
				domainParameterXref,
				domainParameterBindings,
				influencers,
				creationContext
		);
	}

	@Override
	public SimpleSqmDeleteTranslator createSimpleDeleteTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers influencers,
			SqlAstCreationContext creationContext) {
		return new StandardSqmDeleteTranslator(
				creationContext,
				queryOptions,
				domainParameterXref,
				domainParameterBindings
		);
	}

	@Override
	public SqmInsertTranslator createInsertTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers influencers,
			SqlAstCreationContext creationContext) {
		return new StandardSqmInsertTranslator(
				creationContext,
				queryOptions,
				domainParameterXref,
				domainParameterBindings
		);
	}

	@Override
	public SimpleSqmUpdateTranslator createSimpleUpdateTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings queryParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor factory) {
		return new StandardSqmUpdateTranslator(
				factory,
				queryOptions,
				domainParameterXref,
				queryParameterBindings
		);
	}
}
