/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.build.annotations.structure;

import java.util.List;
import javax.lang.model.element.TypeElement;

/**
 * @author Steve Ebersole
 */
public record AnnotationDescriptor(
		TypeElement annotationType,
		String concreteTypeName,
		String constantsClassName,
		String constantName,
		String repeatableContainerConstantName,
		List<AttributeDescriptor> attributes) {
	public String getConstantFqn() {
		return constantsClassName() + "." + constantName();
	}
}
