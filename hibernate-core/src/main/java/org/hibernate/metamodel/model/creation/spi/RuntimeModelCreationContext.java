/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
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

	RuntimeModelNodeFactory getPersisterFactory();
	EntityTuplizerFactory getEntityTuplizerFactory();
	ComponentTuplizerFactory getComponentTuplizerFactory();

	void registerEntityPersister(EntityTypeImplementor entityPersister);
	void registerCollectionPersister(PersistentCollectionMetadata collectionPersister);
	void registerEmbeddablePersister(EmbeddedTypeImplementor embeddablePersister);
}
