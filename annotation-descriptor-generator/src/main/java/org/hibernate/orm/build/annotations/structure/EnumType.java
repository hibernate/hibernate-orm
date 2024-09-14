/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.build.annotations.structure;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.DeclaredType;

/**
 * @author Steve Ebersole
 */
public class EnumType implements Type {
	private final DeclaredType underlyingType;

	public EnumType(DeclaredType underlyingType) {
		this.underlyingType = underlyingType;
	}

	@Override
	public String getTypeDeclarationString() {
		return underlyingType.toString();
	}

	@Override
	public String getInitializerValue(AnnotationValue defaultValue) {
		return underlyingType.toString() + "." + defaultValue.toString();
	}
}
