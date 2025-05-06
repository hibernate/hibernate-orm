/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;

import static org.hibernate.boot.model.internal.BinderHelper.createSyntheticPropertyReference;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * Enable a proper set of the FK columns in respect with the id column order
 * Allow the correct implementation of the default EJB3 values which needs both
 * sides of the association to be resolved
 *
 * @author Emmanuel Bernard
 */
class ToOneFkSecondPass implements FkSecondPass {
	private final PersistentClass persistentClass;
	private final MetadataBuildingContext buildingContext;
	private final boolean unique;
	private final String path;
	private final String entityClassName;
	private final boolean annotatedEntity;
	private final ToOne value;
	private final AnnotatedJoinColumns columns;

	ToOneFkSecondPass(
			ToOne value,
			AnnotatedJoinColumns columns,
			boolean unique,
			boolean annotatedEntity,
			PersistentClass persistentClass,
			String path,
			MetadataBuildingContext buildingContext) {
		this.value = value;
		this.columns = columns;
		this.persistentClass = persistentClass;
		this.buildingContext = buildingContext;
		this.unique = unique;
		this.entityClassName = persistentClass.getClassName();
		this.path = entityClassName != null ? path.substring( entityClassName.length() + 1 ) : path;
		this.annotatedEntity = annotatedEntity;
	}

	@Override
	public SimpleValue getValue() {
		return value;
	}

	@Override
	public String getReferencedEntityName() {
		return value.getReferencedEntityName();
	}

	@Override
	public boolean isInPrimaryKey() {
		if ( entityClassName == null ) {
			return false;
		}
		final PersistentClass persistentClass =
				buildingContext.getMetadataCollector().getEntityBinding( entityClassName );
		final Property property = persistentClass.getIdentifierProperty();
		if ( path == null ) {
			return false;
		}
		else if ( property != null) {
			//try explicit identifier property
			return path.startsWith( property.getName() + "." );
		}
		//try the embedded property
		else {
			if ( persistentClass.getIdentifier() instanceof Component component ) {
				// Embedded property starts their path with 'id.'
				// See PropertyPreloadedData( ) use when idClass != null in AnnotationSourceProcessor
				String localPath = path;
				if ( path.startsWith( "id." ) ) {
					localPath = path.substring( 3 );
				}

				for ( Property idProperty : component.getProperties() ) {
					if ( localPath.equals( idProperty.getName() ) || localPath.startsWith( idProperty.getName() + "." ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void doSecondPass(java.util.Map<String, PersistentClass> persistentClasses) throws MappingException {
		if ( value instanceof ManyToOne manyToOne ) {
			//TODO: move this validation logic to a separate ManyToOneSecondPass
			//      for consistency with how this is handled for OneToOnes
			final String targetEntityName = manyToOne.getReferencedEntityName();
			final PersistentClass targetEntity = persistentClasses.get( targetEntityName );
			if ( targetEntity == null ) {
				final String problem = annotatedEntity
						? " which does not belong to the same persistence unit"
						: " which is not an '@Entity' type";
				throw new AnnotationException( "Association '" + qualify( entityClassName, path )
						+ "' targets the type '" + targetEntityName + "'" + problem );
			}
			manyToOne.setPropertyName( path );
			final String propertyRef = columns.getReferencedProperty();
			if ( propertyRef != null ) {
				handlePropertyRef(
						targetEntity,
						manyToOne,
						path,
						propertyRef,
						buildingContext
				);
			}
			else {
				createSyntheticPropertyReference(
						columns,
						targetEntity,
						persistentClass,
						manyToOne,
						path,
						false,
						buildingContext
				);
			}
			TableBinder.bindForeignKey( targetEntity, persistentClass, columns, manyToOne, unique, buildingContext );
			if ( !manyToOne.isIgnoreNotFound() ) {
				manyToOne.createPropertyRefConstraints( persistentClasses );
			}
		}
		else if ( value instanceof OneToOne ) {
			value.createForeignKey();
		}
		else {
			throw new AssertionFailure( "FkSecondPass for a wrong value type: " + value.getClass().getName() );
		}
	}

	private void handlePropertyRef(
			PersistentClass targetEntity,
			ManyToOne manyToOne,
			String path,
			String referencedPropertyName,
			MetadataBuildingContext buildingContext) {
		manyToOne.setReferencedPropertyName( referencedPropertyName );
		manyToOne.setReferenceToPrimaryKey( false );

		final String entityName = targetEntity.getEntityName();
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		metadataCollector.addUniquePropertyReference( entityName, referencedPropertyName );
		metadataCollector.addPropertyReferencedAssociation( entityName, path, referencedPropertyName );
	}
}
