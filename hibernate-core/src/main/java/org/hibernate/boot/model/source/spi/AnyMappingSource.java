/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Base description for all discriminated associations ("any mappings"), including
 * {@code <any/>}, {@code <many-to-any/>}, etc.
 *
 * @author Steve Ebersole
 */
public interface AnyMappingSource {
	AnyDiscriminatorSource getDiscriminatorSource();
	AnyKeySource getKeySource();

	default boolean isLazy() {
		return true;
	}
}
