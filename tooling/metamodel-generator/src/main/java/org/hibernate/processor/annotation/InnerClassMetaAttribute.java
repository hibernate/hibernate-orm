/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

public class InnerClassMetaAttribute implements MetaAttribute {

	private final AnnotationMeta metaEntity;

	public InnerClassMetaAttribute(AnnotationMeta parent) {
		this.metaEntity = parent;
	}

	public AnnotationMeta getMetaEntity() {
		return metaEntity;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
//		final StringBuilder decl = new StringBuilder()
//				.append("\n/**\n * Static ID class for {@link ")
//				.append( parent.getQualifiedName() )
//				.append( "}\n **/\n" )
//				.append( "public record Id" );
//		String delimiter = "(";
//		for ( MetaAttribute component : components ) {
//			decl.append( delimiter ).append( parent.importType( component.getTypeDeclaration() ) )
//					.append( ' ' ).append( component.getPropertyName() );
//			delimiter = ", ";
//		}
//		return decl.append( ") {}" ).toString();
		return "";
	}

	@Override
	public String getAttributeNameDeclarationString() {
		return "";
	}

	@Override
	public String getMetaType() {
		return "";
	}

	@Override
	public String getPropertyName() {
		return "";
	}

	@Override
	public String getTypeDeclaration() {
		return "";
	}

	@Override
	public Metamodel getHostingEntity() {
		return metaEntity;
	}
}
