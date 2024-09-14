/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.build.annotations.structure;

import java.util.Locale;
import javax.lang.model.element.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public class ArrayType implements Type {
	private final Type componentType;

	public ArrayType(Type componentType) {
		this.componentType = componentType;
	}

	@Override
	public String getTypeDeclarationString() {
		return componentType.getTypeDeclarationString() + "[]";
	}

	@Override
	public String getInitializerValue(AnnotationValue defaultValue) {
		return String.format( Locale.ROOT, "new %s[0]", componentType.getTypeDeclarationString() );
	}
}
