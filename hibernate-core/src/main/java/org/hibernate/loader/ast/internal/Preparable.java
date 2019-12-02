/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

/**
 * Contract for Loader instances wishing to have a "finish initialization" callback after its Loadable
 * has finished initializing.
 *
 * This allows the owner to create the Loader during its construction and trigger its "prepare" later
 *
 * @author Steve Ebersole
 */
public interface Preparable {
	/**
	 * Perform the preparation
	 */
	void prepare();
}
