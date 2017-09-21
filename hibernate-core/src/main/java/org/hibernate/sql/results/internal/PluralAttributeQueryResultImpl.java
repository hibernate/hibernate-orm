/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.InitializerCollector;
import org.hibernate.sql.results.spi.PluralAttributeQueryResult;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeQueryResultImpl
		extends AbstractPluralAttributeMappingNode
		implements PluralAttributeQueryResult {
	public PluralAttributeQueryResultImpl(
			PluralPersistentAttribute pluralAttribute,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		super( pluralAttribute, resultVariable );
	}

	@Override
	public String getResultVariable() {
		return super.getResultVariable();
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		throw new NotYetImplementedFor6Exception(  );
	}
}
