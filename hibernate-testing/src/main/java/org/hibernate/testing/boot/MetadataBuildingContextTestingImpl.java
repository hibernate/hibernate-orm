/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.boot;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.type.internal.TypeDefinitionRegistryImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;

/**
* @author Steve Ebersole
*/
public class MetadataBuildingContextTestingImpl implements MetadataBuildingContext {
	private final MetadataBuildingOptions buildingOptions;
	private final MappingDefaults mappingDefaults;
	private final InFlightMetadataCollector metadataCollector;
	private final BootstrapContext bootstrapContext;
	private final ObjectNameNormalizer objectNameNormalizer;
	private final TypeDefinitionRegistryImpl typeDefinitionRegistry;

	public MetadataBuildingContextTestingImpl() {
		this( new StandardServiceRegistryBuilder().build() );
	}

	public MetadataBuildingContextTestingImpl(StandardServiceRegistry serviceRegistry) {
		buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		bootstrapContext = new BootstrapContextImpl(
				serviceRegistry,
				null,
				buildingOptions
		);
		mappingDefaults = new MetadataBuilderImpl.MappingDefaultsImpl( serviceRegistry );
		metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext,buildingOptions );

		objectNameNormalizer = new ObjectNameNormalizer() {
			@Override
			protected MetadataBuildingContext getBuildingContext() {
				return MetadataBuildingContextTestingImpl.this;
			}
		};
		this.typeDefinitionRegistry = new TypeDefinitionRegistryImpl( bootstrapContext.getTypeConfiguration() );
	}

	@Override
	public TypeDefinition resolveTypeDefinition(String typeName) {
		return typeDefinitionRegistry.resolve( typeName );
	}

	@Override
	public void addTypeDefinition(TypeDefinition typeDefinition) {
		typeDefinitionRegistry.register( typeDefinition );
	}


	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return buildingOptions;
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		return metadataCollector;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return null;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return 0;
	}

	@Override
	public ObjectNameNormalizer getObjectNameNormalizer() {
		return objectNameNormalizer;
	}
}
