/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.query.sqm.produce.spi.AbstractSqmFromBuilder;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.from.MutableUsageDetails;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * Base class for SqmFromBuilder that are part of the FROM clause
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFromBuilderFromClause extends AbstractSqmFromBuilder {
	private final String alias;

	public AbstractSqmFromBuilderFromClause(
			String alias,
			SqmCreationContext sqmCreationContext) {
		super( sqmCreationContext );
		this.alias = alias;
	}

	protected String getAlias() {
		return alias;
	}

	protected void commonHandling(SqmFrom generatedSqmFrom) {
		registerAlias( generatedSqmFrom );
		( (MutableUsageDetails) generatedSqmFrom.getUsageDetails() ).usedInFromClause();
	}
}
