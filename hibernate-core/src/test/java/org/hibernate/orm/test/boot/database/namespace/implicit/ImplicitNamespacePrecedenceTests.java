/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.boot.database.namespace.implicit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.source.internal.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;

import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class ImplicitNamespacePrecedenceTests {
	@Test
	public void testNone() {
		testNone( true );
		testNone( false );
	}

	private void testNone(boolean forceGeneratorTable) {
		ServiceRegistryScope.using(
				() -> new StandardServiceRegistryBuilder().build(),
				(scope) -> {
					final MetadataSources metadataSources = new MetadataSources( scope.getRegistry() );
					metadataSources.addAnnotatedClass( AnEntity.class );

					verify(
							() -> metadataSources,
							scope.getRegistry(),
							!forceGeneratorTable,
							null,
							null
					);
				}
		);
	}

	@Test
	public void testSetting() {
		testSetting( true );
		testSetting( false );
	}

	private void testSetting(boolean forceGeneratorTable) {
		ServiceRegistryScope.using(
				() -> new StandardServiceRegistryBuilder()
						.applySetting( AvailableSettings.DEFAULT_SCHEMA, "setting_schema" )
						.applySetting( AvailableSettings.DEFAULT_CATALOG, "setting_catalog" )
						.build(),
				(ssrScope) -> {
					final StandardServiceRegistry ssr = ssrScope.getRegistry();

					verify(
							() -> new MetadataSources( ssr ).addAnnotatedClass( AnEntity.class ),
							ssr,
							!forceGeneratorTable,
							"setting_schema",
							"setting_catalog"
					);
				}
		);
	}

	@Test
	public void testMapping() {
		testMapping( true );
		testMapping( false );
	}

	private void testMapping(boolean forceGeneratorTable) {
		ServiceRegistryScope.using(
				() -> new StandardServiceRegistryBuilder().build(),
				(ssrScope) -> {
					final StandardServiceRegistry ssr = ssrScope.getRegistry();

					verify(
							() -> new MetadataSources( ssr )
									.addAnnotatedClass( AnEntity.class )
									.addResource( "/mappings/db/namespace/implicit-namespace-mapping.xml" ),
							ssr,
							!forceGeneratorTable,
							"mapping_schema",
							"mapping_catalog"
					);
				}
		);
	}

	@Test
	public void testBoth() {
		testBoth( true );
		testBoth( false );
	}

	private void testBoth(boolean forceGeneratorTable) {
		ServiceRegistryScope.using(
				() -> new StandardServiceRegistryBuilder()
						.applySetting( AvailableSettings.DEFAULT_SCHEMA, "setting_schema" )
						.applySetting( AvailableSettings.DEFAULT_CATALOG, "setting_catalog" )
						.build(),
				(ssrScope) -> {
					final StandardServiceRegistry ssr = ssrScope.getRegistry();

					verify(
							() -> new MetadataSources( ssr )
									.addAnnotatedClass( AnEntity.class )
									.addResource( "/mappings/db/namespace/implicit-namespace-mapping.xml" ),
							ssr,
							!forceGeneratorTable,
							"setting_schema",
							"setting_catalog"
					);
				}
		);
	}

	private void verify(
			Supplier<MetadataSources> metadataSupplier,
			StandardServiceRegistry serviceRegistry,
			boolean physicalSequence,
			String expectedSchema,
			String expectedCatalog) {
		final MetadataSources metadataSources = metadataSupplier.get();

		final Metadata metadata;
		if ( !physicalSequence ) {
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, options );
			options.setBootstrapContext( bootstrapContext );

			final ManagedResources managedResources = MetadataBuildingProcess.prepare(
					metadataSources,
					bootstrapContext
			);

			final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(
					bootstrapContext,
					options
			);

			final MetadataBuildingContextRootImpl rootMetadataBuildingContext = new MetadataBuildingContextRootImpl(
					"orm",
					bootstrapContext,
					options,
					metadataCollector
			);

			bootstrapContext.getTypeConfiguration().scope( rootMetadataBuildingContext );

			final AnnotationMetadataSourceProcessorImpl processor = new AnnotationMetadataSourceProcessorImpl(
					managedResources,
					rootMetadataBuildingContext,
					null
			);
			processor.prepare();

			processor.processTypeDefinitions();
			processor.processQueryRenames();
			processor.processAuxiliaryDatabaseObjectDefinitions();

			processor.processIdentifierGenerators();
			processor.processFilterDefinitions();
			processor.processFetchProfiles();

			final Set<String> processedEntityNames = new HashSet<>();
			processor.prepareForEntityHierarchyProcessing();
			processor.processEntityHierarchies( processedEntityNames );
			processor.postProcessEntityHierarchies();

			processor.processResultSetMappings();
			processor.processNamedQueries();

			processor.finishUp();

			metadataCollector.processSecondPasses( rootMetadataBuildingContext );

			final PersistentClass entityBinding = metadataCollector.getEntityBinding( AnEntity.class.getName() );
			( (SimpleValue) entityBinding.getIdentifier() )
					.getIdentifierGeneratorProperties()
					.put( SequenceStyleGenerator.FORCE_TBL_PARAM, true );

			metadata = metadataCollector.buildMetadataInstance( rootMetadataBuildingContext );
		}
		else {
			metadata = metadataSources.buildMetadata();
		}

		final Database database = metadata.getDatabase();

		// make the SessionFactory to make sure all database objects get registered
//		try (final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
			final Iterator<Namespace> namespaceItr = database.getNamespaces().iterator();

			Namespace registeredNamespace = null;
			while ( namespaceItr.hasNext() ) {
				final Namespace namespace = namespaceItr.next();

				if ( registeredNamespace != null ) {
					// we want to make sure there are no further
					// non-empty namespaces
					if ( CollectionHelper.isNotEmpty( namespace.getTables() )
							|| CollectionHelper.isNotEmpty( namespace.getTables() ) ) {
						fail( "Only expecting one namespace : `" + registeredNamespace + "` & `" + namespace + "`" );
					}
					continue;
				}

				if ( CollectionHelper.isEmpty( namespace.getTables() )
						&& CollectionHelper.isEmpty( namespace.getTables() ) ) {
					continue;
				}

				registeredNamespace = namespace;
			}

			assertThat( registeredNamespace ).isNotNull();

			if ( expectedSchema == null ) {
				assertThat( registeredNamespace.getPhysicalName().getSchema() ).isNull();
			}
			else {
				assertThat( registeredNamespace.getPhysicalName().getSchema().getText() ).isEqualTo( expectedSchema );
			}

			if ( expectedCatalog == null ) {
				assertThat( registeredNamespace.getPhysicalName().getCatalog() ).isNull();
			}
			else {
				assertThat( registeredNamespace.getPhysicalName().getCatalog().getText() ).isEqualTo( expectedCatalog );
			}

			if ( physicalSequence ) {
				assertThat( registeredNamespace.getTables() ).hasSize( 2 );
				assertThat( registeredNamespace.getSequences() ).hasSize( 1 );
			}
			else {
				assertThat( registeredNamespace.getTables() ).hasSize( 3 );
				assertThat( registeredNamespace.getSequences() ).hasSize( 0 );
			}
//		}
	}

	@Entity
	public static class AnEntity {
		@Id
		@GeneratedValue( generator = "id_sequence" )
		private Integer id;
		private String name;
		@ElementCollection
		private Set<String> aliases;

		private AnEntity() {
		}

		public AnEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
			this.aliases = new HashSet<>();
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<String> getAliases() {
			return aliases;
		}

		public void setAliases(Set<String> aliases) {
			this.aliases = aliases;
		}
	}
}
