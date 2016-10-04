/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import java.lang.annotation.Annotation;

import javassist.CtClass;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;

public class UnloadedCtClass implements UnloadedClass {

	private final CtClass ctClass;

	public UnloadedCtClass(CtClass ctClass) {
		this.ctClass = ctClass;
	}

	@Override
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		return ctClass.hasAnnotation( annotationType );
	}

	@Override
	public String getName() {
		return ctClass.getName();
	}
}
