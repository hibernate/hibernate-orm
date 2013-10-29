/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.graph.internal.advisor;

import org.jboss.logging.Logger;

import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.loader.plan.internal.LoadPlanImpl;
import org.hibernate.loader.plan.spi.CopyContext;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.visit.ReturnGraphVisitationStrategy;
import org.hibernate.loader.spi.LoadPlanAdvisor;

/**
 * A LoadPlanAdvisor implementation for applying JPA "entity graph" fetches
 *
 * @author Steve Ebersole
 */
public class EntityGraphBasedLoadPlanAdvisor implements LoadPlanAdvisor {
	private static final Logger log = Logger.getLogger( EntityGraphBasedLoadPlanAdvisor.class );

	private final EntityGraphImpl root;
	private final AdviceStyle adviceStyle;

	/**
	 * Constricts a LoadPlanAdvisor for applying any additional fetches needed as indicated by the
	 * given entity graph.
	 *
	 * @param root The entity graph indicating the fetches.
	 * @param adviceStyle The style of advise (this is defikned
	 */
	public EntityGraphBasedLoadPlanAdvisor(EntityGraphImpl root, AdviceStyle adviceStyle) {
		if ( root == null ) {
			throw new IllegalArgumentException( "EntityGraph cannot be null" );
		}
		this.root = root;
		this.adviceStyle = adviceStyle;
	}

	@Override
	public LoadPlan advise(LoadPlan loadPlan) {
		if ( root == null ) {
			log.debug( "Skipping load plan advising: no entity graph was specified" );
		}
		else {
			// for now, lets assume that the graph and the load-plan returns have to match up
			EntityReturn entityReturn = findRootEntityReturn( loadPlan );
			if ( entityReturn == null ) {
				log.debug( "Skipping load plan advising: not able to find appropriate root entity return in load plan" );
			}
			else {
				final String entityName = entityReturn.getEntityPersister().getEntityName();
				if ( ! root.appliesTo( entityName ) ) {
					log.debugf(
							"Skipping load plan advising: entity types did not match : [%s] and [%s]",
							root.getEntityType().getName(),
							entityName
					);
				}
				else {
					// ok to apply the advice
					return applyAdvice( entityReturn );
				}
			}
		}

		// return the original load-plan
		return loadPlan;
	}

	private LoadPlan applyAdvice(final EntityReturn entityReturn) {
		final EntityReturn copy = entityReturn.makeCopy( new CopyContextImpl( entityReturn ) );
		return new LoadPlanImpl( copy );
	}

	private EntityReturn findRootEntityReturn(LoadPlan loadPlan) {
		EntityReturn rootEntityReturn = null;
		for ( Return rtn : loadPlan.getReturns() ) {
			if ( ! EntityReturn.class.isInstance( rtn ) ) {
				continue;
			}

			if ( rootEntityReturn != null ) {
				log.debug( "Multiple EntityReturns were found" );
				return null;
			}

			rootEntityReturn = (EntityReturn) rtn;
		}

		if ( rootEntityReturn == null ) {
			log.debug( "Unable to find root entity return in load plan" );
		}

		return rootEntityReturn;
	}

	public class CopyContextImpl implements CopyContext {
		private final ReturnGraphVisitationStrategyImpl strategy;

		public CopyContextImpl(EntityReturn entityReturn) {
			strategy = new ReturnGraphVisitationStrategyImpl( entityReturn, root );
		}

		@Override
		public ReturnGraphVisitationStrategy getReturnGraphVisitationStrategy() {
			return strategy;
		}
	}

}
