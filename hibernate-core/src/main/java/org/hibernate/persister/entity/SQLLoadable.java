/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.type.Type;

/**
 * An {@link EntityPersister} that supports queries expressed
 * in the platform native SQL dialect.
 *
 * @author Gavin King, Max Andersen
 *
 * @deprecated Use {@link EntityMappingType}
 */
@Deprecated(since = "6", forRemoval = true)
public interface SQLLoadable extends Loadable {

	/**
	 * Return the column alias names used to persist/query the named property of the class or a subclass (optional operation).
	 */
	String[] getSubclassPropertyColumnAliases(String propertyName, String suffix);

	/**
	 * Return the column names used to persist/query the named property of the class or a subclass (optional operation).
	 */
	String[] getSubclassPropertyColumnNames(String propertyName);
	
	/**
	 * All columns to select, when loading.
	 */
	String selectFragment(String alias, String suffix);

	/**
	 * Get the type
	 */
	Type getType();

}
