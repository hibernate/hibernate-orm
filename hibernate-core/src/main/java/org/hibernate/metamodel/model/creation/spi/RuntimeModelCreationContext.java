/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.collection.spi.PersistentCollectionTuplizerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.internal.PersisterHelper;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.tuple.component.ComponentTuplizerFactory;
import org.hibernate.tuple.entity.EntityTuplizerFactory;
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

	EntityTuplizerFactory getEntityTuplizerFactory();
	ComponentTuplizerFactory getComponentTuplizerFactory();
	PersistentCollectionTuplizerFactory getPersistentCollectionTuplizerFactory();

	void registerEntityPersister(EntityDescriptor entityPersister);
	void registerCollectionPersister(PersistentCollectionDescriptor collectionPersister);
	void registerEmbeddablePersister(EmbeddedTypeDescriptor embeddablePersister);
}
