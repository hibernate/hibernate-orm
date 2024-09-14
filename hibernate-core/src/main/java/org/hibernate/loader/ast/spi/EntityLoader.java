/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * Specialization of Loader for loading entities of a type
 *
 * @author Steve Ebersole
 */
public interface EntityLoader extends Loader {
	@Override
	EntityMappingType getLoadable();
}
