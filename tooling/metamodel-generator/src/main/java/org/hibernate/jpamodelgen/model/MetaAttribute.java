/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.model;

import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * @author Hardy Ferentschik
 */
public interface MetaAttribute {

	String getAttributeDeclarationString();

	String getTypedAttributeDeclarationString(MetaEntity entityForImports, String mtype, List<? extends TypeMirror> toImport);

	String getAttributeDeclarationString(List<? extends TypeParameterElement> typeParameters);

	String getAttributeNameDeclarationString();

	String getType();

	String getMetaType();

	String getPropertyName();

	String getTypeDeclaration();

	MetaEntity getHostingEntity();

}
