/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.xml;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class TypeHelper {
	public static Class<?> resolveClassReference(
			String className,
			XmlDocumentContext xmlDocumentContext,
			Class<?> defaultValue) {
		if ( StringHelper.isEmpty( className ) ) {
			return defaultValue;
		}

		className = xmlDocumentContext.resolveClassName( className );
		return xmlDocumentContext.resolveJavaType( className ).toJavaClass();
	}
}
