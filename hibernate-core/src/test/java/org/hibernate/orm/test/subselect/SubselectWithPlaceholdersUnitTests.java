/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Subselect;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Simple tests that catalog/schema placeholders in {@link org.hibernate.annotations.Subselect}
 * mappings are handled correctly.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SubselectWithPlaceholdersUnitTests {
	@Test
	@DomainModel(annotatedClasses = {EntityInCatalog.class, EntityInCatalogAndSchema.class, EntityInSchema.class, EntityInLocalCatalog.class})
	@SessionFactory(exportSchema = false)
	void testPlaceholdersWithNone(SessionFactoryScope scope) {
		final RuntimeMetamodelsImplementor runtimeMetamodels = scope.getSessionFactory().getRuntimeMetamodels();
		final MappingMetamodelImplementor mappingMetamodel = runtimeMetamodels.getMappingMetamodel();

		final EntityPersister entityInCatalogDescriptor = mappingMetamodel.getEntityDescriptor( EntityInCatalog.class );
		assertThat( entityInCatalogDescriptor.getTableName() ).endsWith( " from table_in_catalog )" );

		final EntityPersister entityInSchemaDescriptor = mappingMetamodel.getEntityDescriptor( EntityInSchema.class );
		assertThat( entityInSchemaDescriptor.getTableName() ).endsWith( " from table_in_schema )" );

		final EntityPersister entityInCatalogAndSchemaDescriptor = mappingMetamodel.getEntityDescriptor( EntityInCatalogAndSchema.class );
		assertThat( entityInCatalogAndSchemaDescriptor.getTableName() ).endsWith( " from table_in_both )" );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name= MappingSettings.DEFAULT_CATALOG, value = "the_catalog")
	})
	@DomainModel(annotatedClasses = {EntityInCatalog.class, EntityInCatalogAndSchema.class, EntityInSchema.class, EntityInLocalCatalog.class})
	@SessionFactory(exportSchema = false)
	void testPlaceholdersWithCatalog(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final RuntimeMetamodelsImplementor runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		final MappingMetamodelImplementor mappingMetamodel = runtimeMetamodels.getMappingMetamodel();

		final EntityPersister entityInCatalogDescriptor = mappingMetamodel.getEntityDescriptor( EntityInCatalog.class );
		assertThat( entityInCatalogDescriptor.getTableName() ).endsWith( " from the_catalog.table_in_catalog )" );

		final EntityPersister entityInSchemaDescriptor = mappingMetamodel.getEntityDescriptor( EntityInSchema.class );
		assertThat( entityInSchemaDescriptor.getTableName() ).endsWith( " from table_in_schema )" );

		final EntityPersister entityInCatalogAndSchemaDescriptor = mappingMetamodel.getEntityDescriptor( EntityInCatalogAndSchema.class );
		assertThat( entityInCatalogAndSchemaDescriptor.getTableName() ).endsWith( " from the_catalog.table_in_both )" );

		final EntityPersister entityInLocalCatalogDescriptor = mappingMetamodel.getEntityDescriptor( EntityInLocalCatalog.class );
		assertThat( entityInLocalCatalogDescriptor.getTableName() ).endsWith( " from local_catalog.table_in_local_catalog )" );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name= MappingSettings.DEFAULT_SCHEMA, value = "the_schema")
	})
	@DomainModel(annotatedClasses = {EntityInCatalog.class, EntityInCatalogAndSchema.class, EntityInSchema.class, EntityInLocalCatalog.class})
	@SessionFactory(exportSchema = false)
	void testPlaceholdersWithSchema(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final RuntimeMetamodelsImplementor runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		final MappingMetamodelImplementor mappingMetamodel = runtimeMetamodels.getMappingMetamodel();


		final EntityPersister entityInCatalogDescriptor = mappingMetamodel.getEntityDescriptor( EntityInCatalog.class );
		assertThat( entityInCatalogDescriptor.getTableName() ).endsWith( " from table_in_catalog )" );

		final EntityPersister entityInSchemaDescriptor = mappingMetamodel.getEntityDescriptor( EntityInSchema.class );
		assertThat( entityInSchemaDescriptor.getTableName() ).endsWith( " from the_schema.table_in_schema )" );

		final EntityPersister entityInCatalogAndSchemaDescriptor = mappingMetamodel.getEntityDescriptor( EntityInCatalogAndSchema.class );
		assertThat( entityInCatalogAndSchemaDescriptor.getTableName() ).endsWith( " from the_schema.table_in_both )" );

		final EntityPersister entityInLocalCatalogDescriptor = mappingMetamodel.getEntityDescriptor( EntityInLocalCatalog.class );
		assertThat( entityInLocalCatalogDescriptor.getTableName() ).endsWith( " from local_catalog.table_in_local_catalog )" );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name= MappingSettings.DEFAULT_CATALOG, value = "the_catalog"),
			@Setting(name= MappingSettings.DEFAULT_SCHEMA, value = "the_schema")
	})
	@DomainModel(annotatedClasses = {EntityInCatalog.class, EntityInCatalogAndSchema.class, EntityInSchema.class, EntityInLocalCatalog.class})
	@SessionFactory(exportSchema = false)
	void testPlaceholdersWithCatalogAndSchema(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final RuntimeMetamodelsImplementor runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		final MappingMetamodelImplementor mappingMetamodel = runtimeMetamodels.getMappingMetamodel();

		final EntityPersister entityInCatalogDescriptor = mappingMetamodel.getEntityDescriptor( EntityInCatalog.class );
		assertThat( entityInCatalogDescriptor.getTableName() ).endsWith( " from the_catalog.table_in_catalog )" );

		final EntityPersister entityInSchemaDescriptor = mappingMetamodel.getEntityDescriptor( EntityInSchema.class );
		assertThat( entityInSchemaDescriptor.getTableName() ).endsWith( " from the_schema.table_in_schema )" );

		final EntityPersister entityInCatalogAndSchemaDescriptor = mappingMetamodel.getEntityDescriptor( EntityInCatalogAndSchema.class );
		assertThat( entityInCatalogAndSchemaDescriptor.getTableName() ).endsWith( " from the_catalog.the_schema.table_in_both )" );

		final EntityPersister entityInLocalCatalogDescriptor = mappingMetamodel.getEntityDescriptor( EntityInLocalCatalog.class );
		assertThat( entityInLocalCatalogDescriptor.getTableName() ).endsWith( " from local_catalog.table_in_local_catalog )" );
	}

	@Entity
	@Subselect("select id, name from {h-catalog}table_in_catalog")
	public static class EntityInCatalog {
		@Id
		private Integer id;
		private String name;
	}

	@Entity
	@Subselect("select id, name from {h-catalog}table_in_local_catalog")
	@Table(catalog = "local_catalog")
	public static class EntityInLocalCatalog {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name = "EntityInCatalogAndSchema")
	@Subselect("select id, name from {h-domain}table_in_both")
	public static class EntityInCatalogAndSchema {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name = "EntityInSchema")
	@Subselect("select id, name from {h-schema}table_in_schema")
	public static class EntityInSchema {
		@Id
		private Integer id;
		private String name;
	}
}
