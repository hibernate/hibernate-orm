/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.annotation.Annotation;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

import net.bytebuddy.description.type.TypeDescription;

class UnloadedTypeDescription implements UnloadedClass {

	private final TypeDescription typeDescription;

	UnloadedTypeDescription(TypeDescription typeDescription) {
		this.typeDescription = typeDescription;
	}

	@Override
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		return typeDescription.getDeclaredAnnotations().isAnnotationPresent( annotationType );
	}

	@Override
	public String getName() {
		return typeDescription.getName();
	}
}
