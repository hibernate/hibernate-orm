package org.hibernate.bytecode.enhance.internal.bytebuddy;

import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;

import jakarta.persistence.Embedded;
import jakarta.persistence.metamodel.Type;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import org.hibernate.bytecode.enhance.spi.UnsupportedEnhancementStrategy;

import static org.hibernate.bytecode.enhance.internal.bytebuddy.PersistentAttributeTransformer.collectPersistentFields;

class ByteBuddyEnhancementContext {

	private final ByteBuddyEnhancementSession session;
	private final EnhancementModel model;
	private final EnhancementOptions options;
	private final EnhancerImplConstants constants;

	ByteBuddyEnhancementContext(ByteBuddyEnhancementSession session, EnhancementOptions options) {
		this.session = session;
		this.model = session.model;
		this.options = Objects.requireNonNull(options);
		this.constants = session.byteBuddyState.getEnhancerConstants();
	}

	public boolean isEntityClass(TypeDescription classDescriptor) {
		return model.isEntityClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isCompositeClass(TypeDescription classDescriptor) {
		return session.discoveredTypes.get(classDescriptor.getName()) == Type.PersistenceType.EMBEDDABLE
				|| model.isCompositeClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isMappedSuperclassClass(TypeDescription classDescriptor) {
		return model.isMappedSuperclassClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean doDirtyCheckingInline() {
		return options.doDirtyCheckingInline();
	}

	public boolean doExtendedEnhancement() {
		return options.doExtendedEnhancement();
	}

	public boolean hasLazyLoadableAttributes(TypeDescription classDescriptor) {
		return options.doLazyInitialization() && model.hasLazyLoadableAttributes( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isPersistentField(AnnotatedFieldDescription field) {
		return model.isPersistentField( field );
	}

	public boolean isCompositeField(AnnotatedFieldDescription field) {
		return isCompositeClass( field.getType().asErasure() ) || field.hasAnnotation( Embedded.class );
	}

	public AnnotatedFieldDescription[] order(AnnotatedFieldDescription[] persistentFields) {
		return (AnnotatedFieldDescription[]) model.order( persistentFields );
	}

	public boolean isLazyLoadable(AnnotatedFieldDescription field) {
		return options.doLazyInitialization() && model.isLazyLoadable( field );
	}

	public boolean isMappedCollection(AnnotatedFieldDescription field) {
		return model.isMappedCollection( field );
	}

	public boolean doBiDirectionalAssociationManagement() {
		return options.doBiDirectionalAssociationManagement();
	}

	public boolean isDiscoveredType(TypeDescription typeDescription) {
		return session.discoveredTypes.containsKey(typeDescription.getName());
	}

	public void registerDiscoveredType(TypeDescription typeDescription, Type.PersistenceType type) {
		session.discoveredTypes.put(typeDescription.getName(), type);
	}

	public UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
		return options.getUnsupportedEnhancementStrategy();
	}

	public void discoverCompositeTypes(TypeDescription type, TypePool typePool) {
		discoverCompositeTypes(type, typePool, false);
	}

	void discoverCompositeTypes(TypeDescription type, TypePool typePool, boolean scheduled) {
		synchronized (session) {
			discoverCompositeTypes(type, typePool, scheduled,
					scheduled ? new java.util.HashSet<>() : session.completedDiscovery,
					new java.util.HashSet<>());
		}
	}

	private void discoverCompositeTypes(TypeDescription managedCtClass, TypePool typePool,
			boolean scheduled, java.util.Set<String> visited, java.util.Set<String> scheduledVisits) {
		final var determinedPersistenceType = determinePersistenceType( managedCtClass );
		scheduled = scheduled && determinedPersistenceType != Type.PersistenceType.BASIC
				&& (determinedPersistenceType != Type.PersistenceType.ENTITY
						|| session.candidates.contains(managedCtClass.getName()));
		// A type first reached through an excluded entity may later be reached
		// through a scheduled embedding. Revisit its graph to propagate scheduling.
		if ( (scheduled ? scheduledVisits : visited).add( managedCtClass.getName() ) ) {
			registerDiscoveredType( managedCtClass, determinedPersistenceType );
			if ( determinedPersistenceType != Type.PersistenceType.BASIC ) {
				if ( scheduled ) {
					session.candidates.add( managedCtClass.getName() );
				}
				for (var parent = managedCtClass.getSuperClass(); parent != null; parent = parent.getSuperClass()) {
					if (isMappedSuperclassClass(parent.asErasure())) {
						discoverCompositeTypes(parent.asErasure(), typePool, scheduled, visited, scheduledVisits);
					}
				}
				final var enhancedFields =
						collectPersistentFields( managedCtClass, this, typePool, constants )
								.getEnhancedFields();
				for ( var enhancedField : enhancedFields ) {
					final var type = enhancedField.getType().asErasure();
					if ( !type.isInterface() && enhancedField.hasAnnotation( Embedded.class ) ) {
						registerDiscoveredType( type, Type.PersistenceType.EMBEDDABLE );
					}
					discoverCompositeTypes( type, typePool, scheduled, visited, scheduledVisits );
				}
			}
		}
	}

	private Type.PersistenceType determinePersistenceType(TypeDescription managedCtClass) {
		if ( isEntityClass( managedCtClass ) ) {
			return Type.PersistenceType.ENTITY;
		}
		else if ( isCompositeClass( managedCtClass ) ) {
			return Type.PersistenceType.EMBEDDABLE;
		}
		else if ( isMappedSuperclassClass( managedCtClass ) ) {
			return Type.PersistenceType.MAPPED_SUPERCLASS;
		}
		else {
			// Default to assuming a basic type if this is not a managed type
			return Type.PersistenceType.BASIC;
		}
	}

	Optional<MethodDescription> resolveGetter(FieldDescription field) {
		return session.metadata.resolveGetter(field);
	}
}
