/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader subtype for processing Query (HQL, Criteria, etc) results.
 *
 * @author Steve Ebersole
 */
public interface QueryLoader extends Loader {
	// todo (6.0) - any additional Options info?
	// 		- e.g. parameters, etc?  Or pass those in as explicit args?

	interface Options {
	}

	List load(SharedSessionContractImplementor session, Options options);
}
