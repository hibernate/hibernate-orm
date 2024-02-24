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
		return new StringBuilder()
				.append("\n/**\n * @see ")
				.append( parent.getQualifiedName() )
				.append("#")
				.append( element.getSimpleName() )
				.append("\n **/\n")
				.append("public static volatile ")
				.append( parent.importType( getMetaType() ) )
				.append("<")
				.append( parent.importType( parent.getQualifiedName() ) )
				.append(", ")
				.append( parent.importType(keyType) )
				.append(", ")
				.append( parent.importType( getTypeDeclaration() ) )
				.append("> ")
				.append( getPropertyName() )
				.append(";")
				.toString();
	}
}
