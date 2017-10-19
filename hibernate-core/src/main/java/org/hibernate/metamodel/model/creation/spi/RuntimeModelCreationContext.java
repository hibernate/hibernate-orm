/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.hibernate.boot.model.domain.EmbeddedMapping;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.collection.spi.PersistentCollectionTuplizerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.model.domain.internal.EntityHierarchyImpl;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.RepresentationStrategySelector;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * "Parameter object" providing access to additional information that may be needed
 * in the creation of the persisters.
 *
 * @author Steve Ebersole
 */
public interface RuntimeModelCreationContext {
	SessionFactoryImplementor getSessionFactory();

	BootstrapContext getBootstrapContext();

	TypeConfiguration getTypeConfiguration();
	MetadataImplementor getMetadata();

	DatabaseModel getDatabaseModel();
	DatabaseObjectResolver getDatabaseObjectResolver();

	JpaStaticMetaModelPopulationSetting getJpaStaticMetaModelPopulationSetting();

	RuntimeModelDescriptorFactory getRuntimeModelDescriptorFactory();
	RepresentationStrategySelector getRepresentationStrategySelector();

	default IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return getSessionFactory().getServiceRegistry().getService( MutableIdentifierGeneratorFactory.class );
	}

	PersistentCollectionTuplizerFactory getPersistentCollectionTuplizerFactory();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : I think these are no longer needed given the way we build these runtime descriptors
	//		it happens within the process itself

	void registerEntityHierarchy(EntityHierarchyImpl runtimeHierarchy, EntityMappingHierarchy bootHierarchy);
	void registerEntityDescriptor(EntityDescriptor runtimeDescriptor, EntityMapping bootDescriptor);
	void registerMappedSuperclassDescriptor(MappedSuperclassDescriptor runtimeType);

	void registerCollectionDescriptor(PersistentCollectionDescriptor runtimeDescriptor, Collection bootDescriptor);

	void registerEmbeddableDescriptor(EmbeddedTypeDescriptor runtimeDescriptor, EmbeddedMapping bootDescriptor);

}
