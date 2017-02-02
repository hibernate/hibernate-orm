/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

/**
 * An avg() projection
 *
 * @author Gavin King
 */
public class AvgProjection extends AggregateProjection {
	/**
	 * Constructs the AvgProjection
	 *
	 * @param propertyName The name of the property to average
	 */
	public AvgProjection(String propertyName) {
		super( "avg", propertyName );
	}
}
