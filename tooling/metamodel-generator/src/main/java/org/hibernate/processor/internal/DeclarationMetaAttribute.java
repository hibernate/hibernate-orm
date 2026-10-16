package org.hibernate.processor.internal;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

/**
 * A member of a metamodel class given as the source code of its declaration,
 * as contributed by {@link org.hibernate.processor.spi.AnnotationMetaEntityContext#addMember(String, String)}.
 */
public class DeclarationMetaAttribute implements MetaAttribute {

	private final Metamodel metamodel;
	private final String name;
	private final String declaration;

	public DeclarationMetaAttribute(Metamodel metamodel, String name, String declaration) {
		this.metamodel = metamodel;
		this.name = name;
		this.declaration = declaration;
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
		return declaration;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		return "";
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException( "operation not supported" );
	}

	@Override
	public String getPropertyName() {
		return name;
	}

	@Override
	public String getTypeDeclaration() {
		return "";
	}

	@Override
	public Metamodel getHostingEntity() {
		return metamodel;
	}
}
