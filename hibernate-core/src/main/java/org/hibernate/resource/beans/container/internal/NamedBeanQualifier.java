/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.internal;

import javax.inject.Named;

/**
 * Used to locate named CDI beans.
 *
 * @author Yoann Rodière
 * @author Steve Ebersole
 */
public class NamedBeanQualifier extends javax.enterprise.util.AnnotationLiteral<Named> implements Named {
	private final String name;

	NamedBeanQualifier(String name) {
		this.name = name;
	}

	@Override
	public String value() {
		return name;
	}
}
