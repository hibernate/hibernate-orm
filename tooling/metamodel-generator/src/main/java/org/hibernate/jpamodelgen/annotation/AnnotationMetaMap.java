/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpamodelgen.annotation;

import javax.lang.model.element.Element;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaMap extends AnnotationMetaCollection {

	private final String keyType;

	public AnnotationMetaMap(AnnotationMetaEntity parent, Element element, String collectionType,
			String keyType, String elementType) {
		super( parent, element, collectionType, elementType );
		this.keyType = keyType;
	}

	public String getDeclarationString() {
		return "public static volatile " + getHostingEntity().importType( getMetaType() )
				+ "<" + getHostingEntity().importType( getHostingEntity().getQualifiedName() )
				+ ", " + getHostingEntity().importType( keyType ) + ", "
				+ getHostingEntity().importType( getTypeDeclaration() ) + "> "
				+ getPropertyName() + ";";
	}
}
