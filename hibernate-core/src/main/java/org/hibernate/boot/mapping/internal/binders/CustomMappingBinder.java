/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.lang.annotation.Annotation;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.binder.AttributeBindingContext;
import org.hibernate.binder.EmbeddableBindingContext;
import org.hibernate.binder.EntityBindingContext;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.mapping.internal.model.EmbeddableContribution;
import org.hibernate.boot.mapping.spi.AttributeApplication;
import org.hibernate.boot.mapping.spi.AttributeDeclaration;
import org.hibernate.boot.mapping.spi.AttributeMetadata;
import org.hibernate.boot.mapping.spi.AttributeUsage;
import org.hibernate.boot.mapping.spi.CategorizedDomainModel;
import org.hibernate.boot.mapping.spi.EntityHierarchy;
import org.hibernate.boot.mapping.spi.EntityTypeMetadata;
import org.hibernate.boot.mapping.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.mapping.spi.DeclarationRole;
import org.hibernate.boot.mapping.spi.MappingRole;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.internal.util.GenericsHelper.typeArguments;

/**
 * Invokes user-defined mapping binders declared through Hibernate annotations.
 */
public class CustomMappingBinder {
	public static ComponentBindingPhase.CustomMapping typeBinding(
			EmbeddableContribution contribution,
			Component component,
			BindingState bindingState,
			BindingContext bindingContext) {
		return new ComponentTypeBinding( contribution, component, bindingState, bindingContext );
	}

	public static AttributeBindingPhase.CustomMapping attributeBinding(
			MemberDetails member,
			PersistentClass persistentClass,
			Property property,
			BindingState bindingState,
			BindingContext bindingContext) {
		return new AttributeBinding( member, persistentClass, property, bindingState, bindingContext );
	}

	static void callTypeBinders(
			org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadataImpl entityType,
			PersistentClass persistentClass,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MetadataBuildingContext metadataBuildingContext = bindingState.getMetadataBuildingContext();
		for ( var metaAnnotated : entityType.getClassDetails().getMetaAnnotated(
				TypeBinderType.class,
				bindingContext.getModelsContext()
		) ) {
			callTypeBinder(
					metaAnnotated,
					metaAnnotated.annotationType(),
					new StandardEntityBindingContext(
							bindingContext.getCategorizedDomainModel(),
							entityType,
							persistentClass,
							metadataBuildingContext
					)
			);
		}
	}

	static void callTypeBinders(
			EmbeddableContribution contribution,
			Component component,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MetadataBuildingContext metadataBuildingContext = bindingState.getMetadataBuildingContext();
		for ( var metaAnnotated : contribution.componentType().getMetaAnnotated(
				TypeBinderType.class,
				bindingContext.getModelsContext()
		) ) {
			callTypeBinder(
					metaAnnotated,
					metaAnnotated.annotationType(),
					new StandardEmbeddableBindingContext(
							bindingContext.getCategorizedDomainModel(),
							contribution.usage(),
							component.getOwner(),
							component,
							metadataBuildingContext
					)
			);
		}
	}

	public static void callAttributeBinders(
			MemberDetails member,
			PersistentClass persistentClass,
			Property property,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( persistentClass == null ) {
			return;
		}

		final AttributeBindingContext attributeBindingContext =
				attributeBindingContext( member, persistentClass, property, bindingState, bindingContext );
		for ( var metaAnnotated : member.getMetaAnnotated(
				AttributeBinderType.class,
				bindingContext.getModelsContext()
		) ) {
			if ( metaAnnotated.annotationType() == TenantId.class
					|| metaAnnotated.annotationType() == Collate.class ) {
				continue;
			}
			callAttributeBinder( metaAnnotated, metaAnnotated.annotationType(), attributeBindingContext );
		}
	}

	private record ComponentTypeBinding(
			EmbeddableContribution contribution,
			Component component,
			BindingState bindingState,
			BindingContext bindingContext) implements ComponentBindingPhase.CustomMapping {
		@Override
		public void bindCustomMapping() {
			callTypeBinders( contribution, component, bindingState, bindingContext );
		}
	}

	private record AttributeBinding(
			MemberDetails member,
			PersistentClass persistentClass,
			Property property,
			BindingState bindingState,
			BindingContext bindingContext) implements AttributeBindingPhase.CustomMapping {
		@Override
		public void bindCustomMapping() {
			callAttributeBinders( member, persistentClass, property, bindingState, bindingContext );
		}
	}

	private static <A extends Annotation> void callTypeBinder(
			Annotation annotation,
			Class<A> annotationType,
			EntityBindingContext context) {
		try {
			typeBinder( annotationType ).bind( annotationType.cast( annotation ), context );
		}
		catch (Exception e) {
			if ( e instanceof AnnotationException annotationException ) {
				throw annotationException;
			}
			throw new AnnotationException(
					"Error processing @TypeBinderType annotation '%s' for entity type '%s'"
							.formatted( annotation, context.getPersistentClass().getClassName() ),
					e
			);
		}
	}

	private static <A extends Annotation> void callTypeBinder(
			Annotation annotation,
			Class<A> annotationType,
			EmbeddableBindingContext context) {
		try {
			typeBinder( annotationType ).bind( annotationType.cast( annotation ), context );
		}
		catch (Exception e) {
			if ( e instanceof AnnotationException annotationException ) {
				throw annotationException;
			}
			throw new AnnotationException(
					"Error processing @TypeBinderType annotation '%s' for embeddable type '%s'"
							.formatted( annotation, context.getComponent().getComponentClassName() ),
					e
			);
		}
	}

	private static <A extends Annotation> void callAttributeBinder(
			Annotation annotation,
			Class<A> annotationType,
			AttributeBindingContext context) {
		try {
			attributeBinder( annotationType ).bind( annotationType.cast( annotation ), context );
		}
		catch (Exception e) {
			if ( e instanceof AnnotationException annotationException ) {
				throw annotationException;
			}
			throw new AnnotationException(
					"Error processing @AttributeBinderType annotation '%s' for attribute '%s' of entity type '%s'"
							.formatted(
									annotation,
									context.getProperty().getName(),
									context.getPersistentClass().getClassName()
							),
					e
			);
		}
	}

	public static AttributeBindingContext attributeBindingContext(
			MemberDetails member,
			PersistentClass persistentClass,
			Property property,
			BindingState bindingState,
			BindingContext bindingContext) {
		return new StandardAttributeBindingContext(
				bindingContext.getCategorizedDomainModel(),
				resolveAttributeApplication( member, persistentClass, property, bindingState, bindingContext ),
				persistentClass,
				property,
				bindingState.getMetadataBuildingContext()
		);
	}

	private static AttributeApplication resolveAttributeApplication(
			MemberDetails member,
			PersistentClass persistentClass,
			Property property,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MappingRole propertyRole = property.getMappingRole();
		if ( propertyRole != null ) {
			final AttributeApplication registered =
					bindingState.getBootBindingModel().getAppliedAttributeMapping( propertyRole );
			if ( registered != null ) {
				return registered;
			}
		}

		final EntityTypeMetadata entityType =
				findEntityType( persistentClass, bindingContext.getCategorizedDomainModel() );
		final AttributeMetadata attribute = findAttribute( entityType, member, property.getName() );
		final MappingRole role = propertyRole == null
				? MappingRole.entity( persistentClass.getEntityName() ).appendAttribute( property.getName() )
				: propertyRole;
		final AccessType accessType = entityType == null ? AccessType.FIELD : entityType.getAccessType();
		final TypeDetails resolvedType = attribute == null
				? member.getType()
				: attribute.resolveAttributeType( entityType.getClassDetails() );
		final var declaration = new BinderAttributeDeclaration(
				new DeclarationRole( member.getDeclaringType().getName(), property.getName() ),
				property.getName(),
				member,
				accessType,
				attribute == null ? org.hibernate.boot.models.AttributeNature.BASIC : attribute.getNature()
		);
		final var usage = new BinderAttributeUsage(
				property.getName(),
				declaration,
				member,
				resolvedType,
				persistentClass.getEntityName() + "." + property.getName(),
				property.getName(),
				attribute == null ? org.hibernate.boot.models.AttributeNature.BASIC : attribute.getNature()
		);
		return new BinderAttributeApplication( usage, role );
	}

	private static EntityTypeMetadata findEntityType(
			PersistentClass persistentClass,
			CategorizedDomainModel domainModel) {
		for ( EntityHierarchy hierarchy : domainModel.getEntityHierarchies() ) {
			final EntityTypeMetadata match = findEntityType( hierarchy.getAbsoluteRoot(), persistentClass );
			if ( match != null ) {
				return match;
			}
		}
		return null;
	}

	private static EntityTypeMetadata findEntityType(
			IdentifiableTypeMetadata candidate,
			PersistentClass persistentClass) {
		if ( candidate instanceof EntityTypeMetadata entity
				&& ( entity.getEntityName().equals( persistentClass.getEntityName() )
						|| entity.getClassDetails().getName().equals( persistentClass.getClassName() ) ) ) {
			return entity;
		}
		for ( IdentifiableTypeMetadata subtype : candidate.getSubTypes() ) {
			final EntityTypeMetadata match = findEntityType( subtype, persistentClass );
			if ( match != null ) {
				return match;
			}
		}
		return null;
	}

	private static AttributeMetadata findAttribute(
			EntityTypeMetadata entityType,
			MemberDetails member,
			String attributeName) {
		for ( IdentifiableTypeMetadata type = entityType; type != null; type = type.getSuperType() ) {
			final AttributeMetadata attribute = type.findAttribute( attributeName );
			if ( attribute != null && attribute.getMember().equals( member ) ) {
				return attribute;
			}
		}
		return null;
	}

	private record StandardAttributeBindingContext(
			CategorizedDomainModel getDomainModel,
			AttributeApplication getAttribute,
			PersistentClass getPersistentClass,
			Property getProperty,
			MetadataBuildingContext getMetadataBuildingContext)
			implements AttributeBindingContext {
	}

	private record StandardEntityBindingContext(
			CategorizedDomainModel getDomainModel,
			EntityTypeMetadata getEntityType,
			PersistentClass getPersistentClass,
			MetadataBuildingContext getMetadataBuildingContext)
			implements EntityBindingContext {
	}

	private record StandardEmbeddableBindingContext(
			CategorizedDomainModel getDomainModel,
			org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata getEmbeddableUsage,
			PersistentClass getPersistentClass,
			Component getComponent,
			MetadataBuildingContext getMetadataBuildingContext)
			implements EmbeddableBindingContext {
	}

	private record BinderAttributeDeclaration(
			DeclarationRole declarationRole,
			String attributeName,
			MemberDetails member,
			AccessType accessType,
			org.hibernate.boot.models.AttributeNature nature)
			implements AttributeDeclaration {
	}

	private record BinderAttributeUsage(
			String attributeName,
			AttributeDeclaration declaration,
			MemberDetails member,
			TypeDetails resolvedType,
			String sourceRole,
			String attributePath,
			org.hibernate.boot.models.AttributeNature nature)
			implements AttributeUsage {
	}

	private record BinderAttributeApplication(AttributeUsage usage, MappingRole role)
			implements AttributeApplication {
	}

	private static <A extends Annotation> TypeBinder<A> typeBinder(Class<A> annotationType)
			throws ReflectiveOperationException {
		final var binderType = annotationType.getAnnotation( TypeBinderType.class ).binder();
		checkImplementedTypeArgument( annotationType, binderType, TypeBinder.class );
		@SuppressWarnings("unchecked")
		final Class<? extends TypeBinder<A>> castBinderType = (Class<? extends TypeBinder<A>>) binderType;
		return castBinderType.getDeclaredConstructor().newInstance();
	}

	private static <A extends Annotation> org.hibernate.binder.AttributeBinder<A> attributeBinder(Class<A> annotationType)
			throws ReflectiveOperationException {
		final var binderType = annotationType.getAnnotation( AttributeBinderType.class ).binder();
		checkImplementedTypeArgument( annotationType, binderType, org.hibernate.binder.AttributeBinder.class );
		@SuppressWarnings("unchecked")
		final Class<? extends org.hibernate.binder.AttributeBinder<A>> castBinderType =
				(Class<? extends org.hibernate.binder.AttributeBinder<A>>) binderType;
		return castBinderType.getDeclaredConstructor().newInstance();
	}

	private static void checkImplementedTypeArgument(
			Class<? extends Annotation> annotationType,
			Class<?> binderType,
			Class<?> implementedType) {
		final var typeArguments = typeArguments( implementedType, binderType );
		if ( typeArguments.length == 1 ) {
			final var requiredAnnotationType = typeArguments[0];
			if ( annotationType != requiredAnnotationType ) {
				throw new AnnotationException(
						"Wrong kind of binder for annotation type: '%s' does not accept an annotation of type '%s'"
								.formatted( binderType.getTypeName(), annotationType.getTypeName() )
				);
			}
		}
	}

	private CustomMappingBinder() {
	}
}
