/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.spi;

/**
 * Contract for the source of DialectResolutionInfo.
 *
 * @author Steve Ebersole
 */
public interface DialectResolutionInfoSource {
	/**
	 * Get the DialectResolutionInfo
	 *
	 * @return The DialectResolutionInfo
	 */
	public DialectResolutionInfo getDialectResolutionInfo();
}
