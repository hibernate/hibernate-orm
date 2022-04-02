/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.MetaEntity;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

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
	public String getTypedAttributeDeclarationString(MetaEntity entityForImports, String mtype, List<? extends TypeMirror> toImport) {
		return "public static volatile " +
				entityForImports.importType(getMetaType()) +
				"<" +
				entityForImports.importType(getHostingEntity().getQualifiedName()) +
				getNonGenericTypesFromTypeMirror(entityForImports, toImport) +
				", " +
				getHostingEntity().importType( keyType ) +
				", " +
				entityForImports.importType(mtype) +
				"> " +
				getPropertyName() +
				";";
	}

	@Override
	public String getAttributeDeclarationString(List<? extends TypeParameterElement> toImport) {
		return "public static volatile " +
				getHostingEntity().importType(getMetaType()) +
				"<" +
				getHostingEntity().importType(getHostingEntity().getQualifiedName()) +
				getNonGenericTypesFromTypeParameter(getHostingEntity(), toImport) +
				", " +
				getHostingEntity().importType( keyType ) +
				", " +
				getHostingEntity().importType(getTypeDeclaration()) +
				"> " +
				getPropertyName() +
				";";
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
