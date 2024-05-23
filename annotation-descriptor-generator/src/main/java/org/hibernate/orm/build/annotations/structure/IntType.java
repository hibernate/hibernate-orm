/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.build.annotations.structure;

import javax.lang.model.element.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public class IntType implements Type {
	public static final IntType INT_TYPE = new IntType();

	@Override
	public String getTypeDeclarationString() {
		return "int";
	}

	@Override
	public String getInitializerValue(AnnotationValue defaultValue) {
		return defaultValue.toString();
	}
}
