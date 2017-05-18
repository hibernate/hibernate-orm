/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import java.io.Serializable;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface CollectionLoader extends Loader {
	// todo (6.0) - any (additional) Options info?

	interface Options {
	}

	List load(Serializable key, SharedSessionContractImplementor session, Options options);
}
