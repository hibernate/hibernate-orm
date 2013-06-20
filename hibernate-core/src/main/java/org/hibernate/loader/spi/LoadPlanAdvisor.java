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
package org.hibernate.loader.spi;

import org.hibernate.loader.plan.spi.LoadPlan;

/**
 * An advisor that can be made available to the {@link org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor} and {@link org.hibernate.loader.plan.exec.process.spi.ScrollableResultSetProcessor}.
 *
 * The processors consult with the advisor, if one is provided, as a means to influence the load plan, meaning that
 * the advisor might add fetches.  A caveat is that any added fetches cannot be join fetches (they cannot alter the
 * SQL); if a fetch is added as {@link org.hibernate.engine.FetchTiming#IMMEDIATE}, it must be a "subsequent form":
 * {@link org.hibernate.engine.FetchStyle#SELECT}, {@link org.hibernate.engine.FetchStyle#SUBSELECT},
 * {@link org.hibernate.engine.FetchStyle#BATCH}.
 *
 * @author Steve Ebersole
 */
public interface LoadPlanAdvisor {
	/**
	 * Advise on the given LoadPlan, returning a new LoadPlan if any additions are needed.  It is the responsibility
	 * of the advisor to return the original load plan if no additions were needed
	 *
	 * @param loadPlan The load plan to advise on.
	 *
	 * @return The original or advised load plan.
	 */
	public LoadPlan advise(LoadPlan loadPlan);
}
