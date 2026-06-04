/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import static org.hibernate.processor.util.Constants.JD_LIFECYCLE_EVENT;
import static org.hibernate.processor.util.Constants.TYPE_LITERAL;

/**
 * A JPA entity listener callback that fires a Jakarta Data lifecycle event.
 *
 * @author Gavin King
 */
public class LifecycleEventCallback implements MetaAttribute {
	private final AnnotationMetaEntity annotationMetaEntity;
	private final String entity;
	private final String callbackAnnotation;
	private final String eventType;

	public LifecycleEventCallback(
			AnnotationMetaEntity annotationMetaEntity,
			String entity,
			String callbackAnnotation,
			String eventType) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.entity = entity;
		this.callbackAnnotation = callbackAnnotation;
		this.eventType = eventType;
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
		annotationMetaEntity.importType( JD_LIFECYCLE_EVENT );
		annotationMetaEntity.importType( TYPE_LITERAL );
		final String entityType = annotationMetaEntity.importType( entity );
		final String eventClass = annotationMetaEntity.importType( "jakarta.data.event." + eventType );
		return new StringBuilder()
				.append( "\n@" )
				.append( annotationMetaEntity.importType( callbackAnnotation ) )
				.append( "\npublic void _on" )
				.append( callbackName() )
				.append( "(" )
				.append( entityType )
				.append( " entity) {\n" )
				.append( "\tif (event != null) {\n" )
				.append( "\t\tevent.select(new TypeLiteral<" )
				.append( eventClass )
				.append( "<" )
				.append( entityType )
				.append( ">>() {})\n" )
				.append( "\t\t\t\t.fire(new " )
				.append( eventClass )
				.append( "<>(entity));\n" )
				.append( "\t}\n" )
				.append( "}" )
				.toString();
	}

	private String callbackName() {
		return eventType.endsWith( "Event" )
				? eventType.substring( 0, eventType.length() - "Event".length() )
				: eventType;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return eventType + '.' + entity;
	}

	@Override
	public String getTypeDeclaration() {
		return entity;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
