/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.exec.spi;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader subtype for loading multiple entities by multiple identifier values.
 *
 * @author Steve Ebersole
 */
public interface MultiIdEntityLoader extends Loader {
	// todo (6.0) - any additional Options info?

	interface Options {
	}

	List load(List ids, SharedSessionContractImplementor session, Options options);
}
