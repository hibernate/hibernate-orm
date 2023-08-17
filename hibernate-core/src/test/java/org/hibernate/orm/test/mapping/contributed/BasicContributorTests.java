/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.contributed;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AdditionalJaxbMappingProducer;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SourceDescriptor;

import org.hibernate.testing.hamcrest.CaseInsensitiveContainsMatcher;
import org.hibernate.testing.hamcrest.CaseInsensitiveStartsWithMatcher;
import org.hibernate.testing.hamcrest.CollectionElementMatcher;
import org.hibernate.testing.orm.JournalingGenerationTarget;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import org.jboss.jandex.IndexView;

import org.hamcrest.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

/**
 * @author Steve Ebersole
 */
@BootstrapServiceRegistry(
		javaServices = @JavaService( role = AdditionalJaxbMappingProducer.class, impl = BasicContributorTests.Contributor.class )
)
@DomainModel( annotatedClasses = BasicContributorTests.MainEntity.class )
public class BasicContributorTests {

	@Test
	public void testContributorFiltering(DomainModelScope scope) {
		final MetadataImplementor metadata = scope.getDomainModel();
		assertThat( metadata.getEntityBindings().size(), Matchers.is( 2 ) );

		final StandardServiceRegistry serviceRegistry = metadata
				.getMetadataBuildingOptions()
				.getServiceRegistry();

		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();

		ExecutionOptions options = new ExecutionOptions() {
			@Override
			public Map getConfigurationValues() {
				return settings;
			}

			@Override
			public boolean shouldManageNamespaces() {
				return false;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return Throwable::printStackTrace;
			}

			@Override
			public SchemaFilter getSchemaFilter() {
				return SchemaFilter.ALL;
			}
		};

		final SchemaManagementTool schemaManagementTool = serviceRegistry.getService( SchemaManagementTool.class );

		final SourceDescriptor sourceDescriptor = new SourceDescriptor() {
			@Override
			public org.hibernate.tool.schema.SourceType getSourceType() {
				return org.hibernate.tool.schema.SourceType.METADATA;
			}

			@Override
			public ScriptSourceInput getScriptSourceInput() {
				return null;
			}
		};

		testDropping( metadata, settings, schemaManagementTool, sourceDescriptor, options );
		testCreating( metadata, settings, schemaManagementTool, sourceDescriptor, options );
	}

	private void testCreating(
			MetadataImplementor metadata,
			Map settings,
			SchemaManagementTool schemaManagementTool,
			SourceDescriptor sourceDescriptor,
			ExecutionOptions options) {

		final SchemaCreatorImpl schemaCreator = (SchemaCreatorImpl) schemaManagementTool.getSchemaCreator( settings );

		final Dialect dialect = new H2Dialect();
		final JournalingGenerationTarget targetDescriptor = new JournalingGenerationTarget();

		// first, unfiltered
		targetDescriptor.clear();
		schemaCreator.doCreation( metadata, dialect, options, contributed -> true, sourceDescriptor, targetDescriptor );
		assertThat(
				targetDescriptor.getCommands(),
				CollectionElementMatcher.hasAllOf(
						CaseInsensitiveContainsMatcher.contains( "main_table" ),
						CaseInsensitiveContainsMatcher.contains( "DynamicEntity" )
				)
		);

		// filter by `orm`
		targetDescriptor.clear();
		schemaCreator.doCreation( metadata, dialect, options, contributed -> "orm".equals( contributed.getContributor() ), sourceDescriptor, targetDescriptor );
		assertThat(
				targetDescriptor.getCommands(),
				CollectionElementMatcher.hasAllOf( not( CaseInsensitiveContainsMatcher.contains( "DynamicEntity" ) ) )
		);

		// filter by `test`
		targetDescriptor.clear();
		schemaCreator.doCreation( metadata, dialect, options, contributed -> "test".equals( contributed.getContributor() ), sourceDescriptor, targetDescriptor );
		assertThat(
				targetDescriptor.getCommands(),
				CollectionElementMatcher.hasAllOf( not( CaseInsensitiveContainsMatcher.contains( "main_table" ) ) )
		);
	}

	private void testDropping(
			MetadataImplementor metadata,
			Map settings,
			SchemaManagementTool schemaManagementTool,
			SourceDescriptor sourceDescriptor, ExecutionOptions options) {

		final SchemaDropperImpl schemaDropper = (SchemaDropperImpl) schemaManagementTool.getSchemaDropper( settings );

		final JournalingGenerationTarget targetDescriptor = new JournalingGenerationTarget();
		final Dialect dialect = new H2Dialect();

		// first, unfiltered
		targetDescriptor.clear();
		schemaDropper.doDrop( metadata, options, contributed -> true, dialect, sourceDescriptor, targetDescriptor );
		assertThat(
				targetDescriptor.getCommands(),
				CollectionElementMatcher.hasAllOf(
						CaseInsensitiveStartsWithMatcher.startsWith( "drop table if exists main_table" ),
						CaseInsensitiveStartsWithMatcher.startsWith( "drop table if exists DynamicEntity" )
				)
		);

		// filter by `orm`
		targetDescriptor.clear();
		schemaDropper.doDrop( metadata, options, contributed -> "orm".equals( contributed.getContributor() ), dialect, sourceDescriptor, targetDescriptor );
		assertThat(
				targetDescriptor.getCommands(),
				CollectionElementMatcher.hasAllOf( not( CaseInsensitiveStartsWithMatcher.startsWith( "drop table DynamicEntity" ) ) )
		);

		// filter by `test`
		targetDescriptor.clear();
		schemaDropper.doDrop( metadata, options, contributed -> "test".equals( contributed.getContributor() ), dialect, sourceDescriptor, targetDescriptor );
		assertThat(
				targetDescriptor.getCommands(),
				CollectionElementMatcher.hasAllOf( not( CaseInsensitiveStartsWithMatcher.startsWith( "drop table main_table" ) ) )
		);

	}

	@Entity( name = "MainEntity" )
	 @Table( name = "main_table" )
	static class MainEntity {
		@Id
		private Integer id;
		String name;

		private MainEntity() {
		}

		public MainEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class Contributor implements AdditionalJaxbMappingProducer {
		public Contributor() {
		}

		@Override
		public Collection<MappingDocument> produceAdditionalMappings(
				MetadataImplementor metadata,
				IndexView jandexIndex,
				MappingBinder mappingBinder,
				MetadataBuildingContext buildingContext) {
			return Collections.singletonList( createMappingDocument( mappingBinder, buildingContext ) );
		}

		private MappingDocument createMappingDocument(MappingBinder mappingBinder, MetadataBuildingContext buildingContext) {
			final Origin origin = new Origin( SourceType.OTHER, "test" );

			final ClassLoaderService classLoaderService = buildingContext.getBootstrapContext()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			final InputStream inputStream = classLoaderService.locateResourceStream( "org/hibernate/orm/test/mapping/contributed/BasicContributorTests.hbm.xml" );
			final Binding<JaxbHbmHibernateMapping> jaxbBinding = mappingBinder.bind( inputStream, origin );
			final JaxbHbmHibernateMapping jaxbRoot = jaxbBinding.getRoot();

			return new MappingDocument(
					"test",
					jaxbRoot,
					origin,
					buildingContext
			);
		}
	}
}
