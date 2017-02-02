/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
