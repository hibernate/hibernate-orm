/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class BootModelPreprocessor {
	static void preprocessBooModel(
			List<Binding<JaxbHbmHibernateMapping>> hbmXmlBindings,
			MetadataImplementor bootModel,
			ServiceRegistry serviceRegistry,
			TransformationState transformationState) {
		bootModel.getEntityBindings().forEach( (persistentClass) -> {
			final Table table = TransformationHelper.determineEntityTable( persistentClass );
			final EntityTypeInfo entityTypeInfo = new EntityTypeInfo( table, persistentClass );
			transformationState.getEntityInfoByName().put( persistentClass.getEntityName(), entityTypeInfo );
			buildPersistentClassPropertyInfos( persistentClass, entityTypeInfo, bootModel, transformationState );
		} );
	}

	private static void buildPersistentClassPropertyInfos(
			PersistentClass persistentClass,
			EntityTypeInfo entityTypeInfo,
			MetadataImplementor bootModel,
			TransformationState transformationState) {
		if ( persistentClass instanceof RootClass rootClass ) {
			if ( persistentClass.getIdentifierProperty() != null ) {
				if ( persistentClass.getIdentifierProperty().getValue() instanceof Component component ) {
					final String componentRole = rootClass.getEntityName() + "." + persistentClass.getIdentifierProperty().getName();
					buildComponentEntries( componentRole, component, transformationState );
				}
			}
			else {
				assert rootClass.getIdentifier() instanceof Component;
				final String componentRole = rootClass.getEntityName() + ".id";
				buildComponentEntries( componentRole, (Component) rootClass.getIdentifier(), transformationState );
			}
		}

		persistentClass.getProperties().forEach( (property) -> processProperty(
				entityTypeInfo.propertyInfoMap(),
				property,
				persistentClass.getEntityName(),
				transformationState
		) );

		persistentClass.getJoins().forEach( (join) -> {
			join.getProperties().forEach( (property) -> processProperty(
					entityTypeInfo.propertyInfoMap(),
					property,
					persistentClass.getEntityName(),
					transformationState
			) );
		} );
	}

	private static void processProperty(
			Map<String, PropertyInfo> entityTypeInfo,
			Property property,
			String entityName,
			TransformationState transformationState) {
		entityTypeInfo.put( property.getName(), new PropertyInfo( property ) );

		if ( property.getValue() instanceof Component component ) {
			final String componentRole = entityName + "." + property.getName();
			buildComponentEntries( componentRole, component, transformationState );
		}
		else if ( property.getValue() instanceof IndexedCollection indexedCollection ) {
			if ( indexedCollection.getIndex() instanceof Component index ) {
				final String componentRole = entityName + "." + property.getName() + ".key";
				buildComponentEntries( componentRole, index, transformationState );
			}
			if ( indexedCollection.getElement() instanceof Component element ) {
				final String componentRole = entityName + "." + property.getName() + ".value";
				buildComponentEntries( componentRole, element, transformationState );
			}
		}
		else if ( property.getValue() instanceof Collection collection ) {
			if ( collection.getElement() instanceof Component element ) {
				final String componentRole = entityName + "." + property.getName() + ".value";
				buildComponentEntries( componentRole, element, transformationState );
			}
		}
	}

	private static void buildComponentEntries(
			String role,
			Component component,
			TransformationState transformationState) {
		final ComponentTypeInfo componentTypeInfo = new ComponentTypeInfo( component );
		transformationState.getEmbeddableInfoByRole().put( role, componentTypeInfo );

		buildComponentPropertyInfos( role, component, componentTypeInfo, transformationState );
	}

	private static void buildComponentPropertyInfos(
			String componentRole,
			Component component,
			ComponentTypeInfo componentTypeInfo,
			TransformationState transformationState) {

		component.getProperties().forEach( (property) -> {
			processProperty( componentTypeInfo.propertyInfoMap(), property, componentRole, transformationState );
		} );

	}
}
