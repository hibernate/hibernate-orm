/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	@Override
	public String getAttributeDeclarationString() {
		return "public static volatile " + getHostingEntity().importType( getMetaType() )
				+ "<" + getHostingEntity().importType( getHostingEntity().getQualifiedName() )
				+ ", " + getHostingEntity().importType( keyType ) + ", "
				+ getHostingEntity().importType( getTypeDeclaration() ) + "> "
				+ getPropertyName() + ";";
	}
}
