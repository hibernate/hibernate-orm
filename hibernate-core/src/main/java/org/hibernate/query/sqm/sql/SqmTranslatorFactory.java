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
import org.hibernate.sql.ast.spi.SqlAstCreationContext;

/**
 * Factory for various
 * @author Steve Ebersole
 */
public interface SqmTranslatorFactory {
	SqmSelectTranslator createSelectTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers influencers,
			SqlAstCreationContext creationContext);

	SimpleSqmDeleteTranslator createSimpleDeleteTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext);

	SqmInsertTranslator createInsertTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers influencers,
			SqlAstCreationContext creationContext);

	SimpleSqmUpdateTranslator createSimpleUpdateTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings queryParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor factory);


	// todo (6.0) : update, delete, etc converters...
}
