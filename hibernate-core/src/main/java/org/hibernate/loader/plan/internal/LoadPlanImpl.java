/*
 * jDocBook, processing of DocBook sources
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
package org.hibernate.loader.plan.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;

/**
 * Implementation of LoadPlan.
 *
 * @author Steve Ebersole
 */
public class LoadPlanImpl implements LoadPlan {
	private final List<? extends Return> returns;
	private final Disposition disposition;
	private final boolean areLazyAttributesForceFetched;

	protected LoadPlanImpl(List<? extends Return> returns, Disposition disposition, boolean areLazyAttributesForceFetched) {
		this.returns = returns;
		this.disposition = disposition;
		this.areLazyAttributesForceFetched = areLazyAttributesForceFetched;
	}

	public LoadPlanImpl(EntityReturn rootReturn) {
		this( Collections.singletonList( rootReturn ), Disposition.ENTITY_LOADER, false );
	}

	public LoadPlanImpl(CollectionReturn rootReturn) {
		this( Collections.singletonList( rootReturn ), Disposition.ENTITY_LOADER, false );
	}

	public LoadPlanImpl(List<? extends Return> returns, boolean areLazyAttributesForceFetched) {
		this( returns, Disposition.MIXED, areLazyAttributesForceFetched );
	}

	@Override
	public List<? extends Return> getReturns() {
		return returns;
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
