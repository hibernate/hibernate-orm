/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.spi;

/**
 * An FetchInitializer is a specialized Initializer for a ResolvedFetch.
 * It adds an additional read phase for linking the fetched value into
 * the owner.
 *
 * @author Steve Ebersole
 */
public interface FetchInitializer extends Initializer {
	InitializerParent getInitializerParent();
	// todo : make sure this is called as one of the last phases in row processing
	void link(Object fkValue);
}
