package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

/**
 * A JPA entity listener callback that fires a Jakarta Data lifecycle event.
 *
 * @author Gavin King
 */
public class LifecycleEventCallback implements MetaAttribute {
	private final AnnotationMetaEntity annotationMetaEntity;
	private final String entity;
	private final String callbackAnnotation;
	private final String callbackName;
	private final String fieldName;

	public LifecycleEventCallback(
			AnnotationMetaEntity annotationMetaEntity,
			String entity,
			String callbackAnnotation,
			String callbackName,
			String fieldName) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.entity = entity;
		this.callbackAnnotation = callbackAnnotation;
		this.callbackName = callbackName;
		this.fieldName = fieldName;
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
		final var entityType = annotationMetaEntity.importType( entity );
		final var eventClass = annotationMetaEntity.importType( "jakarta.data.event." + callbackName + "Event" );
		return new StringBuilder()
				.append( "\n@" )
				.append( annotationMetaEntity.importType( callbackAnnotation ) )
				.append( "\npublic void _on" )
				.append( callbackName )
				.append( "(" )
				.append( entityType )
				.append( " entity) {\n" )
				.append( "\tif (" )
				.append( fieldName )
				.append( " != null) {\n\t\t" )
				.append( fieldName )
				.append( ".fire(new " )
				.append( eventClass )
				.append( "<>(entity));\n" )
				.append( "\t}\n" )
				.append( "}" )
				.toString();
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
		return callbackName + "Event." + entity;
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
