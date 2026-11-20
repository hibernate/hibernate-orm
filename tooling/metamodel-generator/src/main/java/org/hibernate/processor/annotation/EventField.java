package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;

import static org.hibernate.processor.util.Constants.EVENT;
import static org.hibernate.processor.util.Constants.INJECT;
import static org.hibernate.processor.util.Constants.JD_LIFECYCLE_EVENT;


/**
 * Holds a reference to the CDI {@code Event} object.
 *
 * @author Gavin King
 */
public class EventField implements MetaAttribute {

	private final AnnotationMetaEntity annotationMetaEntity;
	private final String entity;
	private final String fieldName;

	public EventField(AnnotationMetaEntity annotationMetaEntity, String entity, String fieldName) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.entity = entity;
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
		annotationMetaEntity.importType(INJECT);
		annotationMetaEntity.importType(EVENT);
		annotationMetaEntity.importType(JD_LIFECYCLE_EVENT);
		return "\n@Inject\nprivate Event<LifecycleEvent<" + annotationMetaEntity.importType( entity )
				+ ">> " + fieldName + ";";
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
		return fieldName;
	}

	@Override
	public String getTypeDeclaration() {
		return Constants.ENTITY_MANAGER;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
