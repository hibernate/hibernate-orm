/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import java.lang.annotation.Annotation;

import javassist.CtField;

import org.hibernate.bytecode.enhance.spi.UnloadedField;

public class UnloadedCtField implements UnloadedField {

	final CtField ctField;

	public UnloadedCtField(CtField ctField) {
		this.ctField = ctField;
	}

	@Override
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		return ctField.hasAnnotation( annotationType );
	}
}
