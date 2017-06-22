/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationProcess;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard implementation of Hibernate's extension to the JPA
 * {@link javax.persistence.metamodel.Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelImpl implements MetamodelImplementor, Serializable {
	private final SessionFactoryImplementor sessionFactory;
	private final TypeConfiguration typeConfiguration;

	private final Map<ManagedJavaDescriptor<?>, MappedSuperclassDescriptor<?>> jpaMappedSuperclassTypeMap = new ConcurrentHashMap<>();

	/**
	 * Instantiate the MetamodelImpl.
	 * <p/>
	 * Note that building a fully-functional MetamodelImpl instance is a 2-step process.  The
	 * create instance returned here must still be initialized via call to {@link #initialize}
	 *
	 * @param typeConfiguration The TypeConfiguration to use for this Metamodel
	 */
	public MetamodelImpl(SessionFactoryImplementor sessionFactory, TypeConfiguration typeConfiguration) {
		this.sessionFactory = sessionFactory;
		this.typeConfiguration = typeConfiguration;
	}

	/**
	 * Prepare the MetamodelImpl for use, using the information from the Hibernate mapping model.
	 *
	 * @param mappingMetadata The Hibernate mapping model
	 * @param jpaMetaModelPopulationSetting Should the JPA Metamodel be built as well?
	 */
	public void initialize(
			MetadataImplementor mappingMetadata,
			JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting) {
		assert typeConfiguration.getMetadataBuildingContext() != null;
		new RuntimeModelCreationProcess( sessionFactory, typeConfiguration.getMetadataBuildingContext() ).execute();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityDescriptor entityPersister = getTypeConfiguration().findEntityDescriptor( cls );
		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls );
		}
		return entityPersister;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> ManagedType<X> managedType(Class<X> cls) {
		final EntityDescriptor entityPersister = getTypeConfiguration().findEntityDescriptor( cls );
		if ( entityPersister != null ) {
			return entityPersister;
		}

		final EmbeddedTypeDescriptor embeddablePersister = getTypeConfiguration().findEmbeddablePersister( cls );
		if ( embeddablePersister != null ) {
			return embeddablePersister;
		}

		final MappedSuperclassDescriptor ms = jpaMappedSuperclassTypeMap.get( cls );
		if ( ms != null ) {
			return ms;
		}

		throw new IllegalArgumentException( "Not a managed type: " + cls );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		final EmbeddedTypeDescriptor embeddablePersister = getTypeConfiguration().findEmbeddablePersister( cls );
		if ( embeddablePersister != null ) {
			return embeddablePersister;
		}

		throw new IllegalArgumentException( "Not an embeddable: " + cls );
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		final Set<ManagedType<?>> managedTypes = new HashSet<>();
		managedTypes.addAll( getTypeConfiguration().getEntityDescriptors() );
		managedTypes.addAll( getTypeConfiguration().getEmbeddablePersisters() );
		managedTypes.addAll( jpaMappedSuperclassTypeMap.values() );
		return managedTypes;
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return new HashSet<>( getTypeConfiguration().getEntityDescriptors() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<>( getTypeConfiguration().getEmbeddablePersisters() );
	}

	@Override
	public void close() {
		// anything to do ?
	}

}
