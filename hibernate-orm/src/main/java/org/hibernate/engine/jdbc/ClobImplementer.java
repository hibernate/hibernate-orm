/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

/**
 * Marker interface for non-contextually created {@link java.sql.Clob} instances..
 *
 * @author Steve Ebersole
 */
public interface ClobImplementer {
	/**
	 * Gets access to the data underlying this CLOB.
	 *
	 * @return Access to the underlying data.
	 */
	public CharacterStream getUnderlyingStream();
}
