/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import java.util.Map;
import javax.persistence.EntityGraph;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.internal.MetamodelImpl;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.spi.AbstractRuntimeModel;
import org.hibernate.metamodel.spi.MetamodelImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class InFlightRuntimeModel extends AbstractRuntimeModel {
	private static final Logger log = Logger.getLogger( InFlightRuntimeModel.class );

	public InFlightRuntimeModel(Map<String,String> hqlNameImports) {
		super();
		hqlNameImports.forEach( (k, v) -> getNameImportMap().putIfAbsent( k, v ) );
	}

	public InFlightRuntimeModel(MetadataBuildingContext metadataBuildingContext) {
		this( metadataBuildingContext.getMetadataCollector().getImports() );
	}

	public void addEntityHierarchy(EntityHierarchy hierarchy) {
		final boolean add = getEntityHierarchySet().add( hierarchy );
		if ( add ) {
			log.debugf( "Added EntityHierarchy : %s", hierarchy );
		}
		else {
			log.debugf( "EntityHierarchy [%s] was already registered" );
		}
	}

	public <T> void addEntityDescriptor(EntityTypeDescriptor<T> descriptor) {
		final EntityTypeDescriptor previous = getEntityDescriptorMap().put(
				descriptor.getNavigableRole().getFullPath(),
				descriptor
		);

		if ( previous != null ) {
			log.debugf( "Adding EntityDescriptor [%s] replaced existing registration [%s]", descriptor, previous );
		}
	}

	public <T> void addMappedSuperclassDescriptor(MappedSuperclassTypeDescriptor<T> descriptor) {
		final MappedSuperclassTypeDescriptor previous = getMappedSuperclassDescriptorMap().put(
				descriptor.getNavigableRole().getFullPath(),
				descriptor
		);

		if ( previous != null ) {
			log.debugf( "Adding MappedSuperclassDescriptor [%s] replaced existing registration [%s]", descriptor, previous );
		}
	}

	public <T> void addEmbeddedDescriptor(EmbeddedTypeDescriptor<T> descriptor) {
		final EmbeddedTypeDescriptor previous = getEmbeddedDescriptorMap().put(
				descriptor.getJavaTypeDescriptor().getTypeName(),
				descriptor
		);

		if ( previous != null ) {
			log.debugf( "Adding EmbeddedTypeDescriptor [%s] replaced existing registration [%s]", descriptor, previous );
		}
	}

	public <O,C,E> void addCollectionDescriptor(PersistentCollectionDescriptor<O,C,E> descriptor) {
		final PersistentCollectionDescriptor previous = getCollectionDescriptorMap().put(
				descriptor.getNavigableRole().getFullPath(),
				descriptor
		);

		if ( previous != null ) {
			log.debugf( "Adding PersistentCollectionDescriptor [%s] replaced existing registration [%s]", descriptor, previous );
		}
	}

	public void addNamedRootGraph(String name, RootGraphImplementor rootGraph) {
		final EntityGraph<?> previous = getRootGraphMap().put( name, rootGraph );

		if ( previous != null ) {
			log.debugf( "Adding EntityGraph [%s -> %s] replaced existing registration [%s]", name, rootGraph, previous );
		}
	}

	public MetamodelImplementor complete(
			SessionFactoryImplementor sessionFactory,
			MetadataBuildingContext metadataBuildingContext) {

		return new MetamodelImpl(
				sessionFactory,
				metadataBuildingContext.getBootstrapContext().getTypeConfiguration(),
				this
		);
	}
}
