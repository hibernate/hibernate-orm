/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.Metamodel;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.hibernate.processor.util.Constants.JAVA_OBJECT;
import static org.hibernate.processor.util.Constants.RESULT_SET_MAPPING;
import static org.hibernate.processor.util.Constants.VOID;
import static org.hibernate.processor.util.TypeUtils.getAnnotationValue;
import static org.hibernate.processor.util.TypeUtils.toTypeString;

/**
 * Represents a SQL result set mapping.
 */
class ResultSetMappingMetaAttribute extends NameMetaAttribute {
	private final String prefix;
	private final String resultType;

	public ResultSetMappingMetaAttribute(
			Metamodel annotationMetaEntity,
			AnnotationMirror mapping,
			String name,
			String prefix) {
		super( annotationMetaEntity, name, prefix );
		this.prefix = prefix;
		resultType = resultType( mapping );
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public String getAttributeDeclarationString() {
		final var entity = getHostingEntity();
		return new StringBuilder()
				.append( "\n/**" )
				.append( "\n * The SQL result set mapping named {@value " )
				.append( prefix )
				.append( fieldName() )
				.append( "}" )
				.append( "\n *" )
				.append( "\n * @see " )
				.append( entity.getQualifiedName() )
				.append( "\n **/" )
				.append( "\npublic static volatile " )
				.append( entity.importType( RESULT_SET_MAPPING ) )
				.append( '<' )
				.append( entity.importType( resultType ) )
				.append( "> " )
				.append( resultSetMappingFieldName( getPropertyName() ) )
				.append( ';' )
				.toString();
	}

	private static String resultType(AnnotationMirror mapping) {
		final var resultTypes = new ArrayList<String>();
		annotationArray( mapping, "entities" )
				.forEach( entity -> resultTypes.add( annotationClassName( entity, "entityClass" ) ) );
		annotationArray( mapping, "classes" )
				.forEach( constructor -> resultTypes.add( annotationClassName( constructor, "targetClass" ) ) );
		annotationArray( mapping, "columns" )
				.forEach( column -> resultTypes.add( columnType( column ) ) );
		return switch ( resultTypes.size() ) {
			case 0 -> JAVA_OBJECT;
			case 1 -> resultTypes.get( 0 );
			default -> JAVA_OBJECT + "[]";
		};
	}

	private static String columnType(AnnotationMirror column) {
		final var type = annotationClassNameOrNull( column, "type" );
		return type == null || isVoid( type ) ? JAVA_OBJECT : type;
	}

	private static boolean isVoid(String type) {
		return "void".equals( type ) || VOID.equals( type );
	}

	private static String annotationClassName(AnnotationMirror annotation, String member) {
		final var className = annotationClassNameOrNull( annotation, member );
		return className == null ? JAVA_OBJECT : className;
	}

	private static String annotationClassNameOrNull(AnnotationMirror annotation, String member) {
		final var value = getAnnotationValue( annotation, member );
		if ( value == null ) {
			return null;
		}
		final var annotationValue = value.getValue();
		if ( annotationValue instanceof TypeMirror type ) {
			return type.getKind() == TypeKind.VOID ? "void" : toTypeString( type );
		}
		return annotationValue.toString();
	}

	private static List<AnnotationMirror> annotationArray(AnnotationMirror annotation, String member) {
		final var value = getAnnotationValue( annotation, member );
		if ( value == null ) {
			return emptyList();
		}
		@SuppressWarnings("unchecked")
		final var annotationValues =
				(List<? extends AnnotationValue>) value.getValue();
		final var result = new ArrayList<AnnotationMirror>();
		for ( var annotationValue : annotationValues ) {
			result.add( (AnnotationMirror) annotationValue.getValue() );
		}
		return result;
	}

	private static String resultSetMappingFieldName(String name) {
		final var fieldName = new StringBuilder( "_" );
		name.codePoints()
				.forEach( codePoint -> fieldName.appendCodePoint(
						Character.isJavaIdentifierPart( codePoint ) ? codePoint : '_' ) );
		return fieldName.toString();
	}
}
