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
package org.hibernate.loader.plan.exec.query.spi;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;

/**
 * Provides values for all options that impact the built query.
 *
 * @author Steve Ebersole
 */
public interface QueryBuildingParameters {

	/**
	 * Provides data for options which can influence the SQL query needed to load an
	 * entity.
	 *
	 * @return the load query influencers
	 *
	 * @see LoadQueryInfluencers
	 */
	public LoadQueryInfluencers getQueryInfluencers();

	/**
	 * Gets the batch size.
	 *
	 * @return The batch size.
	 */
	public int getBatchSize();

	// ultimately it would be better to have a way to resolve the LockMode for a given Return/Fetch...

	/**
	 * Gets the lock mode.
	 * @return The lock mode.
	 */
	public LockMode getLockMode();

	/**
	 * Gets the lock options.
	 *
	 * @return The lock options.
	 */
	public LockOptions getLockOptions();
}
