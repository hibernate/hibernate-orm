/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class MappingTools {
	/**
	 * @param componentName Name of the component, that is, name of the property in the entity that references the component.
	 *
	 * @return A prefix for properties in the given component.
	 */
	public static String createComponentPrefix(String componentName) {
		return componentName + "_";
	}

	/**
	 * @param referencePropertyName The name of the property that holds the relation to the entity.
	 *
	 * @return A prefix which should be used to prefix an id mapper for the related entity.
	 */
	public static String createToOneRelationPrefix(String referencePropertyName) {
		return referencePropertyName + "_";
	}

	public static String getReferencedEntityName(Value value) {
		if ( value instanceof ToOne ) {
			return ( (ToOne) value ).getReferencedEntityName();
		}
		else if ( value instanceof OneToMany ) {
			return ( (OneToMany) value ).getReferencedEntityName();
		}
		else if ( value instanceof Collection ) {
			return getReferencedEntityName( ( (Collection) value ).getElement() );
		}

		return null;
	}

	/**
	 * @param value Persistent property.
	 * @return {@code false} if lack of associated entity shall raise an exception, {@code true} otherwise.
	 */
	public static boolean ignoreNotFound(Value value) {
		if ( value instanceof ManyToOne ) {
			return ( (ManyToOne) value ).isIgnoreNotFound();
		}
		else if ( value instanceof OneToMany ) {
			return ( (OneToMany) value ).isIgnoreNotFound();
		}
		return false;
	}
}
