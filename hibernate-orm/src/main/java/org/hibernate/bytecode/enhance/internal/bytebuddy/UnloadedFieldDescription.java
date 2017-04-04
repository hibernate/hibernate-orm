/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.annotation.Annotation;

import org.hibernate.bytecode.enhance.spi.UnloadedField;

import net.bytebuddy.description.field.FieldDescription;

class UnloadedFieldDescription implements UnloadedField {

	final FieldDescription fieldDescription;

	UnloadedFieldDescription(FieldDescription fieldDescription) {
		this.fieldDescription = fieldDescription;
	}

	@Override
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		return fieldDescription.getDeclaredAnnotations().isAnnotationPresent( annotationType );
	}
}
