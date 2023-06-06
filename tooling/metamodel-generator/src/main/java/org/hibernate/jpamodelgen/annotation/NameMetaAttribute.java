/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.StringUtil;

/**
 * @author Gavin King
 */
class NameMetaAttribute implements MetaAttribute {
	private final AnnotationMetaEntity annotationMetaEntity;
	private final String name;
	private final String prefix;

	public NameMetaAttribute(AnnotationMetaEntity annotationMetaEntity, String name, String prefix) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.name = name;
		this.prefix = prefix;
	}

	@Override
	public boolean hasTypedAttribute() {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAttributeNameDeclarationString() {
		return new StringBuilder()
				.append("public static final ")
				.append(annotationMetaEntity.importType(String.class.getName()))
				.append(" ")
				.append(prefix)
				.append(StringUtil.nameToFieldName(name))
				.append(" = ")
				.append("\"")
				.append(name)
				.append("\"")
				.append(";")
				.toString();
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException();
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
	public MetaEntity getHostingEntity() {
		return annotationMetaEntity;
	}
}
