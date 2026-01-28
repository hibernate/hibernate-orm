/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.internal.metadata.reader.AuditedPropertiesHolder;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Helper class that provides a way to resolve the {@code mappedBy} attribute for collections.
 *
 * @author Chris Cranford
 */
public class CollectionMappedByResolver {

	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			EnversMessageLogger.class,
			CollectionMappedByResolver.class.getName(),
			Locale.ROOT
	);

	public static String resolveMappedBy(
			String entityName,
			Collection collection,
			PropertyAuditingData propertyAuditingData,
			String referencedEntityName,
			EnversMetadataBuildingContext buildingContext) {
		final var referencedClass = getReferencedClass( referencedEntityName, buildingContext );
		final ResolverContext resolverContext = new ResolverContext( collection, propertyAuditingData );
		return getMappedBy( entityName, referencedClass, resolverContext, buildingContext );
	}

	public static String resolveMappedBy(
			String entityName,
			Table collectionTable,
			PropertyAuditingData propertyAuditingData,
			String referencedEntityName,
			EnversMetadataBuildingContext buildingContext) {
		final var referencedClass = getReferencedClass( referencedEntityName, buildingContext );
		return getMappedBy(
				entityName,
				referencedClass,
				new ResolverContext( collectionTable, propertyAuditingData ),
				buildingContext
		);
	}

	public static boolean isMappedByKey(
			Collection collection,
			String mappedBy,
			String referencedEntityName,
			EnversMetadataBuildingContext buildingContext) {
		final PersistentClass referencedClass = getReferencedClass( referencedEntityName,
				buildingContext ).getPersistentClass();
		if ( referencedClass != null ) {
			final String keyMappedBy = searchMappedByKey( referencedClass, collection );
			return mappedBy.equals( keyMappedBy );
		}
		return false;
	}

	private static String getMappedBy(
			String entityName,
			ClassAuditingData referencedClass,
			ResolverContext resolverContext,
			EnversMetadataBuildingContext buildingContext) {
		// If there's an @AuditMappedBy specified, returning it directly.
		final String auditMappedBy = resolverContext.propertyAuditingData.getAuditMappedBy();
		if ( auditMappedBy != null ) {
			return auditMappedBy;
		}

		// searching in referenced class
		String mappedBy = searchMappedBy( referencedClass, resolverContext );

		if ( mappedBy == null ) {
			LOG.debugf(
					"Going to search the mapped by attribute for %s in superclasses of entity: %s",
					resolverContext.propertyAuditingData.getName(),
					referencedClass.getEntityName()
			);

			PersistentClass tempClass = referencedClass.getPersistentClass().getSuperclass();
			while ( mappedBy == null && tempClass != null ) {
				final var superclassName = tempClass.getEntityName();
				LOG.debugf( "Searching in superclass: %s", superclassName );
				final var auditingData = buildingContext.getClassesAuditingData().getClassAuditingData( superclassName );
				mappedBy = searchMappedBy( auditingData, resolverContext );
				tempClass = tempClass.getSuperclass();
			}
		}

		if ( mappedBy == null ) {
			throw new EnversMappingException(
					String.format(
							Locale.ROOT,
							"Could not resolve mapped by property for association [%s.%s] in the referenced entity [%s],"
							+ " please ensure that the association is audited on both sides.",
							entityName,
							resolverContext.propertyAuditingData.getName(),
							referencedClass.getEntityName()
					)
			);
		}

		return mappedBy;
	}

	private static String searchMappedBy(ClassAuditingData referencedClass, ResolverContext resolverContext) {
		if ( resolverContext.getCollection() != null ) {
			return searchMappedBy( referencedClass, resolverContext.getCollection() );
		}
		return searchMappedBy( referencedClass, resolverContext.getTable() );
	}

	private static String searchMappedBy(ClassAuditingData referencedClass, Collection collectionValue) {
		final var persistentClass = referencedClass.getPersistentClass();
		final List<Property> assocClassProps = referencedClass.getPersistentClass().getProperties();
		for ( Property property : assocClassProps ) {
			final List<Selectable> assocClassSelectables = property.getValue().getSelectables();
			final List<Selectable> collectionKeySelectables = collectionValue.getKey().getSelectables();
			if ( Objects.equals( assocClassSelectables, collectionKeySelectables ) ) {
				final var propertyName = property.getName();
				// We need to check if the property is audited as well
				return referencedClass.contains( propertyName ) ? propertyName : null;
			}
		}
		// HHH-7625
		// Support ToOne relations with mappedBy that point to an @IdClass key property.
		return searchMappedByKey( persistentClass, collectionValue );
	}

	private static String searchMappedBy(ClassAuditingData referencedClass, Table collectionTable) {
		return searchMappedBy( referencedClass, referencedClass.getPersistentClass().getProperties(), collectionTable );
	}

	private static String searchMappedBy(AuditedPropertiesHolder propertiesHolder, List<Property> properties, Table collectionTable) {
		for ( Property property : properties ) {
			if ( property.getValue() instanceof Collection ) {
				// The equality is intentional. We want to find a collection property with the same collection table.
				//noinspection ObjectEquality
				if ( ((Collection) property.getValue()).getCollectionTable() == collectionTable ) {
					final var propertyName = property.getName();
					// We need to check if the property is audited as well
					return propertiesHolder.contains( propertyName ) ? propertyName : null;
				}
			}
			else if ( property.getValue() instanceof Component component ) {
				// HHH-12240
				// Should we find an embeddable, we should traverse it as well to see if the collection table
				// happens to be an attribute inside the embeddable rather than directly on the entity.
				final var componentName = property.getName();
				final var componentData = propertiesHolder.getPropertyAuditingData( componentName );
				if ( componentData == null ) {
					// If the component is not audited, no need to check sub-properties
					return null;
				}
				else {
					final String mappedBy = searchMappedBy(
							(ComponentAuditingData) componentData,
							component.getProperties(),
							collectionTable
					);
					if ( mappedBy != null ) {
						return property.getName() + "_" + mappedBy;
					}
				}
			}
		}
		return null;
	}

	private static String searchMappedByKey(PersistentClass referencedClass, Collection collectionValue) {
		for ( KeyValue keyValue : referencedClass.getKeyClosure() ) {
			// make sure it's a 'Component' because IdClass is registered as this type.
			if ( keyValue instanceof Component ) {
				final Component component = (Component) keyValue;
				for ( Property property : component.getProperties() ) {
					final List<Selectable> propertySelectables = property.getValue().getSelectables();
					final List<Selectable> collectionSelectables = collectionValue.getKey().getSelectables();
					if ( Objects.equals( propertySelectables, collectionSelectables ) ) {
						return property.getName();
					}
				}
			}
		}
		return null;
	}

	private static ClassAuditingData getReferencedClass(String className, EnversMetadataBuildingContext buildingContext) {
		return buildingContext.getClassesAuditingData().getClassAuditingData( className );
	}

	private static class ResolverContext {
		private final Collection collection;
		private final PropertyAuditingData propertyAuditingData;
		private final Table table;

		public ResolverContext(Collection collection, PropertyAuditingData propertyAuditingData) {
			this.collection = collection;
			this.propertyAuditingData = propertyAuditingData;
			this.table = null;
		}

		public ResolverContext(Table table, PropertyAuditingData propertyAuditingData) {
			this.table = table;
			this.propertyAuditingData = propertyAuditingData;
			this.collection = null;
		}

		public Collection getCollection() {
			return collection;
		}

		public Table getTable() {
			return table;
		}
	}
}
