/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.util.NameTransformer;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;

import java.util.HashMap;
import java.util.Map;

public class JacksonJakartaAnnotationIntrospector extends JacksonAnnotationIntrospector {

	private final EmbeddableMappingType mappingType;
	private final Map<String, EmbeddableMappingTypeWithFlattening> mappingTypeMap = new HashMap<>();

	public JacksonJakartaAnnotationIntrospector(EmbeddableMappingType mappingType) {
		this.mappingType = mappingType;
		resolveEmbeddableTypes( this.mappingType );
	}

	@Override
	public PropertyName findNameForSerialization(Annotated a) {
		Column column = _findAnnotation(a, Column.class);
		if (column != null && !column.name().isEmpty()) {
			return PropertyName.construct(column.name());
		}
		return super.findNameForSerialization(a);
	}

	@Override
	public PropertyName findNameForDeserialization(Annotated a) {
		Column column = _findAnnotation(a, Column.class);
		if (column != null && !column.name().isEmpty()) {
			return PropertyName.construct(column.name());
		}
		return super.findNameForDeserialization(a);
	}
	private String getFieldNameFromGetterName(String fieldName) {
		// we assume that method is get|set<camelCase Nme>
		// 1. we strip out get|set
		// 2. lowercase on first letter
		assert fieldName != null;
		assert fieldName.substring( 0,3 ).equalsIgnoreCase( "get" ) ||
			fieldName.substring( 0,3 ).equalsIgnoreCase( "set" );
		fieldName = fieldName.substring( 3 );
		return Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
	}
	@Override
	public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
		if(member instanceof AnnotatedField) {
			Embeddable embeddable = member.getType().getRawClass().getAnnotation(Embeddable.class);
			if (embeddable != null) {
				String propName = member.getName();
				if(mappingTypeMap.get(propName) != null){
					EmbeddableMappingTypeWithFlattening embeddableMappingTypeWithFlattening =
							mappingTypeMap.get( propName );
					if(embeddableMappingTypeWithFlattening.isShouldFlatten()) {
						return NameTransformer.simpleTransformer( "","" );
					}
				}
			}
		} else if (member instanceof AnnotatedMethod) {
			Embeddable embeddable = member.getType().getRawClass().getAnnotation(Embeddable.class);
			if (embeddable != null) {
				String propName = getFieldNameFromGetterName(member.getName());
				if(mappingTypeMap.get(propName) != null){
					EmbeddableMappingTypeWithFlattening embeddableMappingTypeWithFlattening =
							mappingTypeMap.get( propName );
					if(embeddableMappingTypeWithFlattening.isShouldFlatten()) {
						return NameTransformer.simpleTransformer( "","" );
					}
				}
			}
		}
		return super.findUnwrappingNameTransformer(member);
	}
// theJson
	private void resolveEmbeddableTypes(EmbeddableMappingType embeddableMappingType) {
		AttributeMappingsList attributeMappings = embeddableMappingType.getAttributeMappings();

		for (int i = 0; i < attributeMappings.size(); i++){
			AttributeMapping attributeMapping = attributeMappings.get(i);
			if ( attributeMapping instanceof EmbeddedAttributeMapping embeddedAttributeMapping ) {

				EmbeddableMappingType attributeEmbeddableMappingType = embeddedAttributeMapping.getMappedType();
				SelectableMapping aggregateMapping = attributeEmbeddableMappingType.getAggregateMapping();

				mappingTypeMap.put( attributeMapping.getAttributeName(),
						new EmbeddableMappingTypeWithFlattening(
								attributeEmbeddableMappingType,
								aggregateMapping == null ));

				resolveEmbeddableTypes( attributeEmbeddableMappingType);
			}
		}
	}

	static class EmbeddableMappingTypeWithFlattening {
		private final EmbeddableMappingType embeddableMappingType;
		private final boolean shouldFlatten;

		public EmbeddableMappingTypeWithFlattening(EmbeddableMappingType embeddableMappingType, boolean shouldFlatten) {
			this.embeddableMappingType = embeddableMappingType;
			this.shouldFlatten = shouldFlatten;
		}

		public EmbeddableMappingType getEmbeddableMappingType() {
			return embeddableMappingType;
		}

		public boolean isShouldFlatten() {
			return shouldFlatten;
		}
	}
}
