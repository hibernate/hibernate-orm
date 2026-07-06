/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.ParamDef;
import org.hibernate.Hibernate;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.mapping.internal.relational.PhysicalTable;
import org.hibernate.boot.mapping.internal.relational.SecondaryTable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.SchemaValidationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleBindingCoordinatorTests {
	@Test
	@ServiceRegistry( settingProviders = @SettingProvider(
			settingName = AvailableSettings.PHYSICAL_NAMING_STRATEGY,
			provider = CustomNamingStrategyProvider.class
	) )
	void testIt(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final var bindingState = context.getBindingState();
					final var metadataCollector = context.getMetadataCollector();

					final var filterDefinition = metadataCollector.getFilterDefinition( "by-name" );
					assertThat( filterDefinition ).isNotNull();
					assertThat( filterDefinition.getDefaultFilterCondition() ).isEqualTo( "name = :name" );
					assertThat( filterDefinition.getParameterNames() ).hasSize( 1 );
					final var nameParamJdbcMapping = filterDefinition.getParameterJdbcMapping( "name" );
					assertThat( nameParamJdbcMapping ).isNotNull();
					assertThat( nameParamJdbcMapping.getJdbcJavaType().getJavaType() ).isEqualTo( String.class );

					assertThat( bindingState.getTableCount() ).isEqualTo( 2 );

					final PhysicalTable simpletonsTable = bindingState.getTableByName( "simpletons" );
					assertThat( simpletonsTable.logicalName().render() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.logicalName().getCanonicalName() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.physicalTableName().render() ).isEqualTo( "SIMPLETONS" );
					assertThat( simpletonsTable.physicalTableName().getCanonicalName() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.physicalCatalogName() ).isNull();
					assertThat( simpletonsTable.getPhysicalSchemaName() ).isNull();
					assertThat( simpletonsTable.binding().getComment() ).isEqualTo( "Stupid is as stupid does" );

					final SecondaryTable simpleStuffTable = bindingState.getTableByName( "simple_stuff" );
					assertThat( simpleStuffTable.logicalName().render() ).isEqualTo( "simple_stuff" );
					assertThat( simpleStuffTable.physicalName().render() ).isEqualTo( "SIMPLE_STUFF" );
					assertThat( simpleStuffTable.logicalCatalogName().render() ).isEqualTo( "my_catalog" );
					assertThat( simpleStuffTable.physicalCatalogName().render() ).isEqualTo( "MY_CATALOG" );
					assertThat( simpleStuffTable.logicalSchemaName().render() ).isEqualTo( "my_schema" );
					assertThat( simpleStuffTable.physicalSchemaName().render() ).isEqualTo( "MY_SCHEMA" );
					assertThat( simpleStuffTable.binding().getComment() ).isEqualTo( "Don't sweat it" );

					final var database = metadataCollector.getDatabase();
					final var namespaceItr = database.getNamespaces().iterator();
					final var namespace1 = namespaceItr.next();
					final var namespace2 = namespaceItr.next();
					assertThat( namespaceItr.hasNext() ).isFalse();
					assertThat( namespace1.getTables() ).hasSize( 1 );
					assertThat( namespace2.getTables() ).hasSize( 1 );

					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( SimpleEntity.class.getName() );
					assertThat( entityBinding.isCached() ).isTrue();
					final Column softDeleteColumn = entityBinding.getSoftDeleteColumn();
					assertThat( softDeleteColumn ).isNotNull();
					assertThat( softDeleteColumn.getName() ).isEqualTo( "ACTIVE" );
					assertThat( entityBinding.getFilters() )
							.extracting( filter -> filter.getCondition() )
							.containsExactlyInAnyOrder( "name = :name", "TENANTKEY = :tenantId" );
					assertThat( entityBinding.getCacheRegionName() ).isEqualTo( "my-region" );
					assertThat( entityBinding.getCacheConcurrencyStrategy() )
							.isEqualTo( CacheConcurrencyStrategy.READ_ONLY.toAccessType().getExternalName() );
					assertThat( entityBinding.getPropertyClosure() )
							.extracting( Property::getName )
							.doesNotContain( "ignored" );

					final Property id = entityBinding.getProperty( "id" );
					assertThat( id.getValue().getTable().getName() ).isEqualTo( "SIMPLETONS" );
					final BasicValue idValue = (BasicValue) id.getValue();
					assertThat( ( (Column) (idValue).getColumn() ).getCanonicalName() ).isEqualTo( "id" );
					assertThat( idValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					final Property name = entityBinding.getProperty( "name" );
					assertThat( id.getValue().getTable().getName() ).isEqualTo( "SIMPLETONS" );
					final BasicValue nameValue = (BasicValue) name.getValue();
					assertThat( ( (Column) (nameValue).getColumn() ).getCanonicalName() ).isEqualTo( "name" );
					assertThat( nameValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( String.class );

					final Property data = entityBinding.getProperty( "data" );
					assertThat( data.getValue().getTable().getName() ).isEqualTo( "SIMPLE_STUFF" );
					final BasicValue dataValue = (BasicValue) data.getValue();
					assertThat( ( (Column) (dataValue).getColumn() ).getCanonicalName() ).isEqualTo( "datum" );
					assertThat( dataValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( String.class );

					final Property stuff = entityBinding.getProperty( "stuff" );
					assertThat( stuff.getValue().getTable().getName() ).isEqualTo( "SIMPLETONS" );
					final BasicValue stuffValue = (BasicValue) stuff.getValue();
					assertThat( stuffValue.getEnumerationStyle() ).isEqualTo( EnumType.STRING );
					assertThat( ( (Column) stuffValue.getColumn() ).getCanonicalName() ).isEqualTo( "stuff" );
					assertThat( stuffValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( SimpleEntity.Stuff.class );
					assertThat( stuffValue.resolve().getJdbcType().getJdbcTypeCode() ).isEqualTo( SqlTypes.VARCHAR );

					final Property tenantKey = entityBinding.getProperty( "tenantKey" );
					final BasicValue tenantKeyValue = (BasicValue) tenantKey.getValue();
					assertThat( ( (Column) tenantKeyValue.getColumn() ).getCanonicalName() ).isEqualTo( "tenantkey" );
					assertThat( tenantKeyValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( String.class );

					final Property version = entityBinding.getProperty( "version" );
					final BasicValue versionValue = (BasicValue) version.getValue();
					assertThat( ( (Column) versionValue.getColumn() ).getCanonicalName() ).isEqualTo( "version" );
					assertThat( versionValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );
				},
				scope.getRegistry(),
				SimpleEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testMinimalMetadataBuildsSessionFactory(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					try (var sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( context.getMetadata() )) {
						assertThat( sessionFactory.getMappingMetamodel()
								.getEntityDescriptor( SessionFactoryBuildableEntity.class ) ).isNotNull();
					}
				},
				scope.getRegistry(),
				SessionFactoryBuildableEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testSchemaManagerUsesNewPipelineDatabaseModel(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final var table = context.getMetadataCollector()
							.getDatabase()
							.getDefaultNamespace()
							.locateTable( Identifier.toIdentifier( "schema_export_entities" ) );
					assertThat( table ).isNotNull();
					assertThat( table.getColumn( Identifier.toIdentifier( "name" ) ) ).isNotNull();

					try (var sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( context.getMetadata() )) {
						sessionFactory.getSchemaManager().create( true );
						try {
							validateSchema( sessionFactory );

							try (var session = sessionFactory.openSession()) {
								final var transaction = session.beginTransaction();
								session.persist( new SchemaExportEntity( 1, "one" ) );
								transaction.commit();
							}
						}
						finally {
							sessionFactory.getSchemaManager().drop( true );
						}
					}
				},
				scope.getRegistry(),
				SchemaExportEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testFilterDefinitionsReachRuntimeAndApply(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					try (var sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( context.getMetadata() )) {
						final var activeStatusDefinition = sessionFactory.getFilterDefinition( "activeStatus" );
						assertThat( activeStatusDefinition.getDefaultFilterCondition() ).isEqualTo( "status = 1" );
						assertThat( activeStatusDefinition.isAutoEnabled() ).isTrue();
						assertThat( activeStatusDefinition.isAppliedToLoadByKey() ).isTrue();

						final var byNameDefinition = sessionFactory.getFilterDefinition( "byFilterName" );
						assertThat( byNameDefinition.getDefaultFilterCondition() ).isEqualTo( "name = :name" );
						assertThat( byNameDefinition.isAutoEnabled() ).isFalse();
						assertThat( byNameDefinition.isAppliedToLoadByKey() ).isTrue();
						assertThat( byNameDefinition.getParameterNames() ).containsExactly( "name" );
						assertThat( byNameDefinition.getParameterJdbcMapping( "name" ).getJdbcJavaType().getJavaType() )
								.isEqualTo( String.class );

						sessionFactory.getSchemaManager().create( true );
						try {
							try (var session = sessionFactory.openSession()) {
								final var transaction = session.beginTransaction();
								session.persist( new FilterHandoffEntity( 1, "alpha", 1 ) );
								session.persist( new FilterHandoffEntity( 2, "beta", 1 ) );
								session.persist( new FilterHandoffEntity( 3, "inactive", 0 ) );
								transaction.commit();
							}

							try (var session = sessionFactory.openSession()) {
								assertThat( session.getEnabledFilter( "activeStatus" ) ).isNotNull();
								assertThat( session.find( FilterHandoffEntity.class, 3 ) ).isNull();
								assertThat( session.createQuery(
										"from FilterHandoffEntity order by id",
										FilterHandoffEntity.class
								).getResultList() )
										.extracting( FilterHandoffEntity::getName )
										.containsExactly( "alpha", "beta" );

								session.enableFilter( "byFilterName" ).setParameter( "name", "beta" );
								assertThat( session.createQuery(
										"from FilterHandoffEntity order by id",
										FilterHandoffEntity.class
								).getResultList() )
										.extracting( FilterHandoffEntity::getName )
										.containsExactly( "beta" );
							}
						}
						finally {
							sessionFactory.getSchemaManager().drop( true );
						}
					}
				},
				scope.getRegistry(),
				FilterHandoffEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testFetchProfileAndEntityGraphReachRuntimeAndApply(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					try (var sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( context.getMetadata() )) {
						assertThat( sessionFactory.containsFetchProfileDefinition( "child-with-parent-profile" ) )
								.isTrue();
						assertThat( sessionFactory.findEntityGraphByName( "child-with-parent-graph" ) )
								.isNotNull();

						sessionFactory.getSchemaManager().create( true );
						try {
							try (var session = sessionFactory.openSession()) {
								final var transaction = session.beginTransaction();
								final var parent = new FetchHandoffParent( 1, "parent" );
								session.persist( parent );
								session.persist( new FetchHandoffChild( 1, "child", parent ) );
								transaction.commit();
							}

							try (var session = sessionFactory.openSession()) {
								final var child = session.createQuery(
										"from FetchHandoffChild",
										FetchHandoffChild.class
								).getSingleResult();
								assertThat( Hibernate.isInitialized( child.getParent() ) ).isFalse();
							}

							try (var session = sessionFactory.openSession()) {
								session.enableFetchProfile( "child-with-parent-profile" );
								assertThat( session.isFetchProfileEnabled( "child-with-parent-profile" ) ).isTrue();
								final var child = session.createQuery(
										"from FetchHandoffChild",
										FetchHandoffChild.class
								).getSingleResult();
								assertThat( Hibernate.isInitialized( child.getParent() ) ).isTrue();
							}

							try (var session = sessionFactory.openSession()) {
								final var graph = session.getEntityGraph(
										FetchHandoffChild.class,
										"child-with-parent-graph"
								);
								assertThat( graph.hasAttributeNode( "parent" ) ).isTrue();
								final var child = session.createQuery(
										"from FetchHandoffChild",
										FetchHandoffChild.class
								).setEntityGraph( graph, GraphSemantic.LOAD ).getSingleResult();
								assertThat( Hibernate.isInitialized( child.getParent() ) ).isTrue();
							}
						}
						finally {
							sessionFactory.getSchemaManager().drop( true );
						}
					}
				},
				scope.getRegistry(),
				FetchHandoffParent.class,
				FetchHandoffChild.class
		);
	}

	private static void validateSchema(org.hibernate.SessionFactory sessionFactory) {
		try {
			sessionFactory.getSchemaManager().validate();
		}
		catch (SchemaValidationException e) {
			throw new AssertionError( e );
		}
	}

	@Entity(name = "SessionFactoryBuildableEntity")
	public static class SessionFactoryBuildableEntity {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name = "SchemaExportEntity")
	@jakarta.persistence.Table(name = "schema_export_entities")
	public static class SchemaExportEntity {
		@Id
		private Integer id;

		@jakarta.persistence.Column(nullable = false, length = 40)
		private String name;

		protected SchemaExportEntity() {
		}

		public SchemaExportEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "FilterHandoffEntity")
	@jakarta.persistence.Table(name = "filter_handoff_entities")
	@FilterDef(name = "activeStatus", defaultCondition = "status = 1", autoEnabled = true, applyToLoadByKey = true)
	@FilterDef(
			name = "byFilterName",
			defaultCondition = "name = :name",
			parameters = @ParamDef(name = "name", type = String.class),
			applyToLoadByKey = true
	)
	@Filter(name = "activeStatus")
	@Filter(name = "byFilterName")
	public static class FilterHandoffEntity {
		@Id
		private Integer id;

		private String name;

		private int status;

		protected FilterHandoffEntity() {
		}

		public FilterHandoffEntity(Integer id, String name, int status) {
			this.id = id;
			this.name = name;
			this.status = status;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "FetchHandoffParent")
	@jakarta.persistence.Table(name = "fetch_handoff_parents")
	public static class FetchHandoffParent {
		@Id
		private Integer id;

		private String name;

		protected FetchHandoffParent() {
		}

		public FetchHandoffParent(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "FetchHandoffChild")
	@jakarta.persistence.Table(name = "fetch_handoff_children")
	@NamedEntityGraph(name = "child-with-parent-graph", attributeNodes = @NamedAttributeNode("parent"))
	@FetchProfile(
			name = "child-with-parent-profile",
			fetchOverrides = @FetchProfile.FetchOverride(
					entity = FetchHandoffChild.class,
					association = "parent"
			)
	)
	public static class FetchHandoffChild {
		@Id
		private Integer id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		private FetchHandoffParent parent;

		protected FetchHandoffChild() {
		}

		public FetchHandoffChild(Integer id, String name, FetchHandoffParent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
		}

		public FetchHandoffParent getParent() {
			return parent;
		}
	}
}
