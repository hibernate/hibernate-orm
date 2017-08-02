/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.QueryResultCollection;
import org.hibernate.sql.exec.results.spi.QueryResultCreationContext;
import org.hibernate.sql.exec.results.spi.SqlSelectionResolver;

/**
 * @author Steve Ebersole
 */
public class QueryResultCollectionImpl extends AbstractCollectionReference implements QueryResultCollection {
	public QueryResultCollectionImpl(
			PluralPersistentAttribute pluralAttribute,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		super( pluralAttribute, resultVariable );
	}

	@Override
	public PluralPersistentAttribute getType() {
		return getNavigable();
	}

	@Override
	public String getResultVariable() {
		return super.getResultVariable();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		throw new NotYetImplementedException(  );
	}
}
