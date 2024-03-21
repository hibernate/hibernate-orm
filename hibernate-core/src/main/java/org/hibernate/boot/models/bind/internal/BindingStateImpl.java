/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.models.categorize.spi.TableOwner;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.models.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public class BindingStateImpl implements BindingState {
	private final MetadataBuildingContext metadataBuildingContext;

	private final Database database;
	private final JdbcServices jdbcServices;

	private final Map<String, TableReference> tableMap = new HashMap<>();
	private final Map<TableOwner, TableReference> tableByOwnerMap = new HashMap<>();

	private final Map<ClassDetails, ManagedTypeBinding> typeBindings = new HashMap<>();
	private final Map<ClassDetails, IdentifiableTypeBinding> typeBindersBySuper = new HashMap<>();

	public BindingStateImpl(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.database = metadataBuildingContext.getMetadataCollector().getDatabase();
		this.jdbcServices = metadataBuildingContext.getBootstrapContext().getServiceRegistry().getService( JdbcServices.class );
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	public int getTableCount() {
		return tableMap.size();
	}

	@Override
	public void forEachTable(KeyedConsumer<String,TableReference> consumer) {
		//noinspection unchecked
		tableMap.forEach( (BiConsumer<? super String, ? super TableReference>) consumer );
	}

	@Override
	public <T extends TableReference> T getTableByName(String name) {
		//noinspection unchecked
		return (T) tableMap.get( name );
	}

	@Override
	public <T extends TableReference> T getTableByOwner(TableOwner owner) {
		//noinspection unchecked
		return (T) tableByOwnerMap.get( owner );
	}

	@Override
	public void addTable(TableOwner owner, TableReference table) {
		tableMap.put( table.logicalName().getCanonicalName(), table );
		tableByOwnerMap.put( owner, table );
	}

	@Override
	public void addSecondaryTable(SecondaryTable table) {
		tableMap.put( table.logicalName().getCanonicalName(), table );
	}

	private String resolveSchemaName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		var defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultSchemaName = defaultNamespace.getName().getSchema();
			if ( defaultSchemaName != null ) {
				return defaultSchemaName.getCanonicalName();
			}
		}
		return null;
	}

	private String resolveCatalogName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		var defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultCatalogName = defaultNamespace.getName().getCatalog();
			if ( defaultCatalogName != null ) {
				return defaultCatalogName.getCanonicalName();
			}
		}
		return null;

	}

	@Override
	public void registerTypeBinding(ManagedTypeMetadata type, ManagedTypeBinding binding) {
		typeBindings.put( type.getClassDetails(), binding );

		if ( type instanceof IdentifiableTypeMetadata identifiableType ) {
			if ( identifiableType.getSuperType() != null ) {
				typeBindersBySuper.put(
						identifiableType.getSuperType().getClassDetails(),
						(IdentifiableTypeBinding) binding
				);
			}
		}

		if ( binding instanceof EntityBinding entityTypeBinding ) {
			metadataBuildingContext.getMetadataCollector().addEntityBinding( entityTypeBinding.getPersistentClass() );
		}
		else if ( binding instanceof MappedSuperclassBinding mappedSuperBinding ) {
			metadataBuildingContext.getMetadataCollector().addMappedSuperclass(
					mappedSuperBinding.typeMetadata.getClassDetails().toJavaClass(),
					mappedSuperBinding.getMappedSuperclass()
			);
		}
	}

	@Override
	public ManagedTypeBinding getTypeBinding(ClassDetails type) {
		return typeBindings.get( type );
	}

	@Override
	public IdentifiableTypeBinding getSuperTypeBinding(ClassDetails type) {
		return typeBindersBySuper.get( type );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Filter def

	@Override
	public void apply(FilterDefRegistration registration) {
		metadataBuildingContext.getMetadataCollector().addFilterDefinition( registration.toFilterDefinition( metadataBuildingContext ) );
	}
}
