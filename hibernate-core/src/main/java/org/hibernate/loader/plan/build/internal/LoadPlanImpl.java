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
package org.hibernate.loader.plan.build.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.loader.plan.spi.QuerySpaces;
import org.hibernate.loader.plan.spi.Return;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class LoadPlanImpl implements LoadPlan {
	private static final Logger log = CoreLogging.logger( LoadPlanImpl.class );

	private final List<? extends Return> returns;
	private final QuerySpaces querySpaces;
	private final Disposition disposition;
	private final boolean areLazyAttributesForceFetched;

	protected LoadPlanImpl(
			List<? extends Return> returns,
			QuerySpaces querySpaces,
			Disposition disposition,
			boolean areLazyAttributesForceFetched) {
		this.returns = returns;
		this.querySpaces = querySpaces;
		this.disposition = disposition;
		this.areLazyAttributesForceFetched = areLazyAttributesForceFetched;
	}

	/**
	 * Creates a {@link Disposition#ENTITY_LOADER} LoadPlan.
	 *
	 * @param rootReturn The EntityReturn representation of the entity being loaded.
	 * @param querySpaces The QuerySpaces containing all the {@link QuerySpace} references
	 *                    required for <code>rootReturn</code> and joined entity, collection,
	 *                    and composite references.
	 */
	public LoadPlanImpl(EntityReturn rootReturn, QuerySpaces querySpaces) {
		this( Collections.singletonList( rootReturn ), querySpaces, Disposition.ENTITY_LOADER, false );
	}

	/**
	 * Creates a {@link Disposition#COLLECTION_INITIALIZER} LoadPlan.
	 *
	 * @param rootReturn The CollectionReturn representation of the collection being initialized.
	 * @param querySpaces The QuerySpaces containing all the {@link QuerySpace} references
	 *                    required for <code>rootReturn</code> and joined entity, collection,
	 *                    and composite references.
	 */
	public LoadPlanImpl(CollectionReturn rootReturn, QuerySpaces querySpaces) {
		this( Collections.singletonList( rootReturn ), querySpaces, Disposition.COLLECTION_INITIALIZER, false );
	}

	/**
	 * Creates a {@link Disposition#MIXED} LoadPlan.
	 *
	 * @param returns The mixed Return references
	 * @param querySpaces The QuerySpaces containing all the {@link QuerySpace} references
	 *                    required for <code>rootReturn</code> and joined entity, collection,
	 *                    and composite references.
	 * @param areLazyAttributesForceFetched Should lazy attributes (bytecode enhanced laziness) be fetched also?  This
	 * effects the eventual SQL SELECT-clause which is why we have it here.  Currently this is "all-or-none"; you
	 * can request that all lazy properties across all entities in the loadplan be force fetched or none.  There is
	 * no entity-by-entity option.  {@code FETCH ALL PROPERTIES} is the way this is requested in HQL.  Would be nice to
	 * consider this entity-by-entity, as opposed to all-or-none.  For example, "fetch the LOB value for the Item.image
	 * attribute, but no others (leave them lazy)".  Not too concerned about having it at the attribute level.
	 */
	public LoadPlanImpl(List<? extends Return> returns, QuerySpaces querySpaces, boolean areLazyAttributesForceFetched) {
		this( returns, querySpaces, Disposition.MIXED, areLazyAttributesForceFetched );
	}

	@Override
	public List<? extends Return> getReturns() {
		return returns;
	}

	@Override
	public QuerySpaces getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public Disposition getDisposition() {
		return disposition;
	}

	@Override
	public boolean areLazyAttributesForceFetched() {
		return areLazyAttributesForceFetched;
	}

	@Override
	public boolean hasAnyScalarReturns() {
		return disposition == Disposition.MIXED;
	}
}
