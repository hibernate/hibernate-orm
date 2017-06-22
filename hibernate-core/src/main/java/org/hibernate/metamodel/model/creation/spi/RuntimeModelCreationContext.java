/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.collection.spi.PersistentCollectionTuplizerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.InstantiatorFactory;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
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

	TypeConfiguration getTypeConfiguration();
	MetadataImplementor getMetadata();

	DatabaseModel getDatabaseModel();
	DatabaseObjectResolver getDatabaseObjectResolver();

	RuntimeModelDescriptorFactory getRuntimeModelDescriptorFactory();

	default IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return getSessionFactory().getServiceRegistry().getService( MutableIdentifierGeneratorFactory.class );
	}

	InstantiatorFactory getInstantiatorFactory();

	PersistentCollectionTuplizerFactory getPersistentCollectionTuplizerFactory();

	void registerEntityDescriptor(EntityDescriptor entityDescriptor);
	void registerCollectionDescriptor(PersistentCollectionDescriptor collectionDescriptor);
	void registerEmbeddableDescriptor(EmbeddedTypeDescriptor embeddedTypeDescriptor);
}
