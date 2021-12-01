/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Gavin King
 */
public class EmbeddedComponentType extends ComponentType {
	public EmbeddedComponentType(TypeConfiguration typeScope, ComponentMetamodel metamodel, int[] propertyReordering) {
		super( typeScope, metamodel, propertyReordering );
	}

	public boolean isEmbedded() {
		return true;
	}

	public boolean isMethodOf(Method method) {
		return componentTuplizer.isMethodOf( method );
	}
}
