/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import static org.hibernate.processor.util.StringUtil.nameToFieldName;

/**
 * @author Gavin King
 */
class NameMetaAttribute implements MetaAttribute {
	private final Metamodel annotationMetaEntity;
	private final String name;
	private final String prefix;

	public NameMetaAttribute(Metamodel annotationMetaEntity, String name, String prefix) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.name = name;
		this.prefix = prefix;
	}

	@Override
	public boolean hasTypedAttribute() {
		return false;
	}

	@Override
	public boolean hasStringAttribute() {
		return true;
	}

	@Override
	public String getAttributeDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getAttributeNameDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		if ( !annotationMetaEntity.isJakartaDataStyle() ) {
			declaration.append( "public static final " );
		}
		return declaration
				.append(annotationMetaEntity.importType(String.class.getName()))
				.append(" ")
				.append(prefix)
				.append(fieldName())
				.append(" = ")
				.append("\"")
				.append(name)
				.append("\"")
				.append(";")
				.toString();
	}

	private String fieldName() {
		return nameToFieldName(name.charAt(0) == '#' ? name.substring(1) : name);
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return name;
	}

	@Override
	public String getTypeDeclaration() {
		return "java.lang.String";
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
