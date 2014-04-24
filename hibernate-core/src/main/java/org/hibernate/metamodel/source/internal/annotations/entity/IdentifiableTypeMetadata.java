/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal.annotations.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.persistence.AccessType;

import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.AttributeConversionInfo;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.JpaCallbackSourceImpl;
import org.hibernate.metamodel.source.internal.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAttribute;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPAListenerHelper;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.jandex.PseudoJpaDotNames;
import org.hibernate.metamodel.source.spi.JpaCallbackSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.binding.InheritanceType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * Representation of metadata (configured via annotations or orm.xml) attached
 * to an Entity or a MappedSuperclass.
 *
 * @author Steve Ebersole
 */
public class IdentifiableTypeMetadata extends ManagedTypeMetadata {
	private static final Logger log = Logger.getLogger( IdentifiableTypeMetadata.class );

	private IdType idType;
	private List<SingularAttribute> identifierAttributes;
	private List<SingularAssociationAttribute> mapsIdAssociationAttributes;
	private BasicAttribute versionAttribute;

	private final Map<AttributePath, AttributeConversionInfo> conversionInfoMap = new HashMap<AttributePath, AttributeConversionInfo>();
	private final Map<AttributePath, AttributeOverride> attributeOverrideMap = new HashMap<AttributePath, AttributeOverride>();
	private final Map<AttributePath, AssociationOverride> associationOverrideMap = new HashMap<AttributePath, AssociationOverride>();

	private List<JpaCallbackSource> collectedJpaCallbackSources;

	/**
	 * This form is intended for construction of root Entity, and any of
	 * its MappedSuperclasses
	 *
	 * @param javaTypeDescriptor The Entity/MappedSuperclass class descriptor
	 * @param defaultAccessType The default AccessType for the hierarchy
	 * @param isRoot Is this the root entity?
	 * @param bindingContext The context
	 */
	public IdentifiableTypeMetadata(
			JavaTypeDescriptor javaTypeDescriptor,
			AccessType defaultAccessType,
			boolean isRoot,
			AnnotationBindingContext bindingContext) {
		super( javaTypeDescriptor, defaultAccessType, isRoot, bindingContext );

		// the idea here is to collect up class-level annotations and to apply
		// the maps from supers
		collectConversionInfo();
		collectAttributeOverrides();
		collectAssociationOverrides();
	}

	protected void collectConversionInfo() {
		// we only need to do this on root
	}

	protected void collectAttributeOverrides() {
		// we only need to do this on root
	}

	protected void collectAssociationOverrides() {
		// we only need to do this on root
	}

//		// todo : account for supers/subs...
//		if ( CollectionHelper.isEmpty( getMappedSuperclassTypeMetadatas() ) ) {
//			return RootEntityTypeMetadata.super.getAssociationOverrideMap();
//		}
//		Map<String, AssociationOverride> map = new HashMap<String, AssociationOverride>();
//		for ( MappedSuperclassTypeMetadata mappedSuperclassTypeMetadata : getMappedSuperclassTypeMetadatas() ) {
//			map.putAll( mappedSuperclassTypeMetadata.getAssociationOverrideMap() );
//		}
//		map.putAll( RootEntityTypeMetadata.super.getAssociationOverrideMap() );
//		return map;

	/**
	 * This form is intended for cases where the Entity/MappedSuperclass
	 * is part of the root subclass tree.
	 *
	 * @param javaTypeDescriptor Metadata for the Entity/MappedSuperclass
	 * @param superType The metadata for the super type.
	 * @param defaultAccessType The default AccessType for the entity hierarchy
	 * @param context The binding context
	 */
	public IdentifiableTypeMetadata(
			ClassDescriptor javaTypeDescriptor,
			IdentifiableTypeMetadata superType,
			AccessType defaultAccessType,
			AnnotationBindingContext context) {
		super( javaTypeDescriptor, superType, defaultAccessType, context );

		// the idea here is to collect up class-level annotations and to apply
		// the maps from supers
		collectConversionInfo();
		collectAttributeOverrides();
		collectAssociationOverrides();
	}

	@Override
	public boolean isAbstract() {
		return super.isAbstract();
	}

	@Override
	public AttributeConversionInfo locateConversionInfo(AttributePath attributePath) {
		return conversionInfoMap.get( attributePath );
	}

	@Override
	public AttributeOverride locateAttributeOverride(AttributePath attributePath) {
		return attributeOverrideMap.get( attributePath );
	}

	@Override
	public AssociationOverride locateAssociationOverride(AttributePath attributePath) {
		return associationOverrideMap.get( attributePath );
	}

	@Override
	public IdentifiableTypeMetadata getSuperType() {
		return (IdentifiableTypeMetadata) super.getSuperType();
	}

	/**
	 * Obtain the InheritanceType defined locally within this class
	 *
	 * @return Return the InheritanceType locally defined; {@code null} indicates
	 * no InheritanceType was locally defined.
	 */
	public InheritanceType getLocallyDefinedInheritanceType() {
		final AnnotationInstance inheritanceAnnotation = JandexHelper.getSingleAnnotation(
				getJavaTypeDescriptor().getJandexClassInfo(),
				JPADotNames.INHERITANCE
		);
		if ( inheritanceAnnotation != null ) {
			final AnnotationValue strategyValue = inheritanceAnnotation.value( "strategy" );
			if ( strategyValue != null ) {
				return InheritanceType.valueOf( strategyValue.asEnum() );
			}
			else {
				// the @Inheritance#strategy default value
				return InheritanceType.SINGLE_TABLE;
			}
		}

		return null;
	}

	/**
	 * Obtain all JPA callbacks specifically indicated on the entity itself.  This
	 * includes:<ul>
	 *     <li>local callbacks (methods on the class itself)</li>
	 *     <li>callbacks from @EntityListener annotation on the class itself</li>
	 * </ul>
	 *
	 * @return The callbacks.  {@code null} is never returned
	 */
	public List<JpaCallbackSource> getJpaCallbacks() {
		if ( collectedJpaCallbackSources == null ) {
			collectedJpaCallbackSources = collectJpaCallbacks();
		}
		return collectedJpaCallbackSources;
	}

	private List<JpaCallbackSource> collectJpaCallbacks() {
		final LinkedHashSet<JpaCallbackSource> callbacks = new LinkedHashSet<JpaCallbackSource>();

		// local (to the entity itself) callback
		collectCallbacks( getJavaTypeDescriptor(), false, callbacks );

		// EntityListeners annotation on the entity
		final AnnotationInstance entityListenersAnnotation = JandexHelper.getSingleAnnotation(
				getJavaTypeDescriptor().getJandexClassInfo(),
				JPADotNames.ENTITY_LISTENERS
		);
		if ( entityListenersAnnotation != null ) {
			final Type[] types = entityListenersAnnotation.value().asClassArray();
			for ( Type type : types ) {
				final JavaTypeDescriptor entityListener = getLocalBindingContext().getJavaTypeDescriptorRepository().getType(
						getLocalBindingContext().getJavaTypeDescriptorRepository().buildName( type.name().toString() )
				);
				collectCallbacks( entityListener, true, callbacks );
			}
		}

		// EntityListeners "annotation" defined as default in orm.xml
		final Collection<AnnotationInstance> defaultEntityListenersAnnotations =
				getLocalBindingContext().getJandexAccess().getIndex().getAnnotations( PseudoJpaDotNames.DEFAULT_ENTITY_LISTENERS );
		// there really should be only one or none...
		if ( defaultEntityListenersAnnotations != null ) {
			if ( defaultEntityListenersAnnotations.size() > 1 ) {
				log.debugf(
						"Encountered multiple default <entity-listener/> definitions; merging lists"
				);
			}
			for ( AnnotationInstance defaultEntityListenersAnnotation : defaultEntityListenersAnnotations ) {
				final Type[] types = defaultEntityListenersAnnotation.value().asClassArray();
				for ( Type type : types ) {
					final JavaTypeDescriptor entityListener = getLocalBindingContext().getJavaTypeDescriptorRepository().getType(
							getLocalBindingContext().getJavaTypeDescriptorRepository().buildName( type.name().toString() )
					);
					collectCallbacks( entityListener, true, callbacks );
				}
			}
		}

		return new ArrayList<JpaCallbackSource>( callbacks );
	}

	private void collectCallbacks(
			JavaTypeDescriptor javaTypeDescriptor,
			boolean isListener,
			LinkedHashSet<JpaCallbackSource> callbacks) {
		if ( isListener && !ClassDescriptor.class.isInstance( javaTypeDescriptor ) ) {
			throw getLocalBindingContext().makeMappingException(
					"Callback listener cannot be an interface : " + javaTypeDescriptor.getName().toString()
			);
		}

		final String prePersistCallback = JPAListenerHelper.findCallback(
				javaTypeDescriptor,
				JPADotNames.PRE_PERSIST,
				isListener
		);
		final String preRemoveCallback = JPAListenerHelper.findCallback(
				javaTypeDescriptor,
				JPADotNames.PRE_REMOVE,
				isListener
		);
		final String preUpdateCallback = JPAListenerHelper.findCallback(
				javaTypeDescriptor,
				JPADotNames.PRE_UPDATE,
				isListener
		);
		final String postLoadCallback = JPAListenerHelper.findCallback(
				javaTypeDescriptor,
				JPADotNames.POST_LOAD,
				isListener
		);
		final String postPersistCallback = JPAListenerHelper.findCallback(
				javaTypeDescriptor,
				JPADotNames.POST_PERSIST,
				isListener
		);
		final String postRemoveCallback = JPAListenerHelper.findCallback(
				javaTypeDescriptor,
				JPADotNames.POST_REMOVE,
				isListener
		);
		final String postUpdateCallback = JPAListenerHelper.findCallback(
				javaTypeDescriptor,
				JPADotNames.POST_UPDATE,
				isListener
		);

		if ( prePersistCallback == null
				&& preRemoveCallback == null
				&& preUpdateCallback == null
				&& postLoadCallback == null
				&& postPersistCallback == null
				&& postRemoveCallback == null
				&& postUpdateCallback == null ) {
			if ( isListener ) {
				log.debugf(
						"Entity listener class [%s] named by @EntityListener on entity [%s] contained no callback methods",
						javaTypeDescriptor.getName(),
						getJavaTypeDescriptor().getName()
				);
			}
		}
		else {
			callbacks.add(
					new JpaCallbackSourceImpl(
							javaTypeDescriptor,
							isListener,
							prePersistCallback,
							preRemoveCallback,
							preUpdateCallback,
							postLoadCallback,
							postPersistCallback,
							postRemoveCallback,
							postUpdateCallback
					)
			);
		}
	}

	@Override
	public void registerConverter(
			AttributePath attributePath,
			AttributeConversionInfo conversionInfo) {
		conversionInfoMap.put( attributePath, conversionInfo );
	}

	@Override
	public void registerAttributeOverride(
			AttributePath attributePath,
			AttributeOverride override) {
		if ( attributeOverrideMap.containsKey( attributePath ) ) {
			// an already registered path indicates that a higher context has already
			// done a registration; ignore the incoming one.
			log.debugf(
					"On registration of @AttributeOverride we already had a " +
							"registered override for the given path [%s]; ignoring.  " +
							"This subsequent registration should indicate a 'lower " +
							"precedence' location."
			);
		}
		else {
			attributeOverrideMap.put( attributePath, override );
		}
	}

	@Override
	public void registerAssociationOverride(
			AttributePath attributePath,
			AssociationOverride override) {
		associationOverrideMap.put( attributePath, override );
	}

	@Override
	protected void categorizeAttribute(PersistentAttribute persistentAttribute) {
		if ( SingularAttribute.class.isInstance( persistentAttribute ) ) {
			final SingularAttribute singularAttribute = (SingularAttribute) persistentAttribute;
			if ( singularAttribute.isVersion() ) {
				if ( versionAttribute != null ) {
					throw getLocalBindingContext().makeMappingException(
							String.format(
									Locale.ENGLISH,
									"Multiple attributes [%s, %s] were indicated as Version",
									versionAttribute.getName(),
									singularAttribute.getName()
							)
					);
				}
				if ( singularAttribute.isId() ) {
					throw getLocalBindingContext().makeMappingException(
							String.format(
									Locale.ENGLISH,
									"Attributes [%s] was indicated as Id and as Version",
									singularAttribute.getName()
							)
					);
				}
				// only BasicAttributes can be versions
				versionAttribute = (BasicAttribute) singularAttribute;
				return;
			}

			if ( singularAttribute.isId() ) {
				if ( identifierAttributes == null ) {
					// first collected identifier attribute
					identifierAttributes = new ArrayList<SingularAttribute>();
					switch ( singularAttribute.getNature() ) {
						case EMBEDDED:
						case EMBEDDED_ID: {
							idType = IdType.AGGREGATED;
							break;
						}
						default: {
							idType = IdType.SIMPLE;
							break;
						}
					}
				}
				else {
					// multiple collected identifier attribute
					idType = IdType.NON_AGGREGATED;
				}
				identifierAttributes.add( singularAttribute );
				return;
			}

			if ( SingularAssociationAttribute.class.isInstance( singularAttribute ) ) {
				final SingularAssociationAttribute toOneAttribute = (SingularAssociationAttribute) singularAttribute;
				if ( toOneAttribute.getMapsIdAnnotation() != null ) {
					if ( mapsIdAssociationAttributes == null ) {
						mapsIdAssociationAttributes = new ArrayList<SingularAssociationAttribute>();
					}
					mapsIdAssociationAttributes.add( toOneAttribute );
					return;
				}
			}
		}

		super.categorizeAttribute( persistentAttribute );
	}


	public IdType getIdType() {
		collectAttributesIfNeeded();

		if ( idType == null ) {
			if ( getSuperType() != null ) {
				return getSuperType().getIdType();
			}
		}
		return idType == null ? IdType.NONE : idType;
	}

	public List<SingularAttribute> getIdentifierAttributes() {
		collectAttributesIfNeeded();

		if ( identifierAttributes == null ) {
			if ( getSuperType() != null ) {
				return getSuperType().getIdentifierAttributes();
			}
		}
		return identifierAttributes == null ? Collections.<SingularAttribute>emptyList() : identifierAttributes;
	}

	public List<SingularAssociationAttribute> getMapsIdAttributes() {
		collectAttributesIfNeeded();

		if ( mapsIdAssociationAttributes == null ) {
			return getSuperType() != null
					? getSuperType().getMapsIdAttributes()
					: Collections.<SingularAssociationAttribute>emptyList();
		}
		else {
			return mapsIdAssociationAttributes;
		}
	}

	public BasicAttribute getVersionAttribute() {
		collectAttributesIfNeeded();

		if ( versionAttribute == null ) {
			if ( getSuperType() != null ) {
				return getSuperType().getVersionAttribute();
			}
		}
		return versionAttribute;
	}
}
