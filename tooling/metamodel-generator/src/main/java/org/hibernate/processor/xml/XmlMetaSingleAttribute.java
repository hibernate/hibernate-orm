/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.xml;

import org.hibernate.processor.model.MetaSingleAttribute;
import org.hibernate.processor.util.Constants;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaSingleAttribute extends XmlMetaAttribute implements MetaSingleAttribute {

	public XmlMetaSingleAttribute(XmlMetaEntity parent, String propertyName, String type) {
		super( parent, propertyName, type );
	}

	@Override
	public String getAttributeDeclarationString() {
		if ( !isSingleGenericAttribute() ) {
			return super.getAttributeDeclarationString();
		}
		final var hostingEntity = getHostingEntity();
		return "public static volatile " + hostingEntity.importType( getMetaType() )
				+ "<" + hostingEntity.importType( hostingEntity.getQualifiedName() )
				+ "> " + getPropertyName() + ";";
	}

	@Override
	public String getMetaType() {
		return switch ( getTypeDeclaration() ) {
			case "java.lang.String", "String" -> Constants.TEXT_ATTRIBUTE;
			case "boolean", "java.lang.Boolean", "Boolean" -> Constants.BOOLEAN_ATTRIBUTE;
			case "byte", "short", "int", "long", "float", "double",
					"java.lang.Byte", "Byte",
					"java.lang.Short", "Short",
					"java.lang.Integer", "Integer",
					"java.lang.Long", "Long",
					"java.lang.Float", "Float",
					"java.lang.Double", "Double",
					"java.math.BigInteger", "BigInteger",
					"java.math.BigDecimal", "BigDecimal" -> Constants.NUMERIC_ATTRIBUTE;
			case "java.time.Instant", "Instant",
					"java.time.LocalDate", "LocalDate",
					"java.time.LocalDateTime", "LocalDateTime",
					"java.time.LocalTime", "LocalTime",
					"java.time.OffsetDateTime", "OffsetDateTime",
					"java.time.OffsetTime", "OffsetTime",
					"java.time.ZonedDateTime", "ZonedDateTime" -> Constants.TEMPORAL_ATTRIBUTE;
			case "char", "java.lang.Character", "Character" -> Constants.COMPARABLE_ATTRIBUTE;
			default -> Constants.SINGULAR_ATTRIBUTE;
		};
	}

	private boolean isSingleGenericAttribute() {
		final var metaType = getMetaType();
		return Constants.TEXT_ATTRIBUTE.equals( metaType )
			|| Constants.BOOLEAN_ATTRIBUTE.equals( metaType );
	}
}
