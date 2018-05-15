/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import org.jboss.logging.Logger;


/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFromBuilder implements SqmFromBuilder {
	private static final Logger log = Logger.getLogger( AbstractSqmFromBuilder.class );

	private final SqmCreationContext sqmCreationContext;

	protected AbstractSqmFromBuilder(SqmCreationContext sqmCreationContext) {
		this.sqmCreationContext = sqmCreationContext;
	}

	protected  SqmCreationContext getSqmCreationContext() {
		return sqmCreationContext;
	}

	@Override
	public SqmRoot buildRoot(EntityValuedNavigable navigable) {
		throw new SqmProductionException( "Call to #buildRoot should never happen in this  context" );
	}

	@Override
	public SqmCrossJoin buildCrossJoin(EntityValuedNavigable navigable) {
		throw new SqmProductionException( "Call to #buildCrossJoin should never happen in this  context" );
	}

	@Override
	public SqmEntityJoin buildEntityJoin(EntityValuedNavigable navigable) {
		throw new SqmProductionException( "Call to #buildEntityJoin should never happen in this  context" );
	}

	@Override
	public SqmNavigableJoin buildNavigableJoin(SqmNavigableReference navigableReference) {
		throw new SqmProductionException( "Call to #buildNavigableJoin should never happen in this  context" );
	}

	protected void registerAlias(SqmFrom sqmFrom) {
		final String alias = sqmFrom.getIdentificationVariable();

		if ( alias == null ) {
			throw new ParsingException( "FromElement alias was null" );
		}

		if ( ImplicitAliasGenerator.isImplicitAlias( alias ) ) {
			log.debug( "Alias registration for implicit FromElement alias : " + alias );
		}

		sqmCreationContext.getCurrentQuerySpecProcessingState().getAliasRegistry().registerAlias( sqmFrom );
	}
}
