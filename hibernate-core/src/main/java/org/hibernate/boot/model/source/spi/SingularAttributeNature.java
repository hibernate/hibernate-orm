/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes possible natures of a singular attribute.
 *
 * @author Steve Ebersole
 */
public enum SingularAttributeNature {
	BASIC,
	// TODO: COMPOSITE should be changed to AGGREGATE
	// when non-aggregated composite IDs are no longer
	// modelled as an AttributeBinding
	COMPOSITE,
	MANY_TO_ONE,
	ONE_TO_ONE,
	ANY
}
