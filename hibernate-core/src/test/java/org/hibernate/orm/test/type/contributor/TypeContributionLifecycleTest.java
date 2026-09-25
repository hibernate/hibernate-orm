/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.orm.test.type.contributor.usertype.MyCompositeValue;
import org.hibernate.orm.test.type.contributor.usertype.MyCompositeValueType;
import org.hibernate.orm.test.type.contributor.usertype.StringWrapper;
import org.hibernate.orm.test.type.contributor.usertype.StringWrapperUserType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.CustomType;
import org.hibernate.type.UserComponentType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.internal.NamedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CLASSLOADERS;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET;
import static org.hibernate.jpa.boot.spi.JpaSettings.TYPE_CONTRIBUTORS;

/// Verifies the shared service-contribution lifecycle through the public bootstrap entry points.
///
/// @author Steve Ebersole
@BaseUnitTest
public class TypeContributionLifecycleTest {

	private static final String UNIT_NAME = "type-contribution-lifecycle";
	private static final String TYPE_NAME = "bootstrap_contributed_type";
	private static final ThreadLocal<List<String>> EVENTS = ThreadLocal.withInitial( ArrayList::new );
	private static final NamedBasicTypeImpl<String> LOW_TYPE = type( "low" );
	private static final NamedBasicTypeImpl<String> FIRST_HIGH_TYPE = type( "first-high" );
	private static final NamedBasicTypeImpl<String> LAST_HIGH_TYPE = type( "last-high" );
	private static final NamedBasicTypeImpl<String> EXPLICIT_TYPE = type( "explicit" );
	private static final StringJavaType JAVA_TYPE = new StringJavaType();
	private static final VarcharJdbcType JDBC_TYPE = new VarcharJdbcType();

	@TempDir
	Path directory;

	@AfterEach
	void clearEvents() {
		EVENTS.remove();
	}

	@ParameterizedTest
	@EnumSource(EntryPoint.class)
	void serviceContributionsRunOncePerMetadataConstruction(EntryPoint entryPoint) throws Exception {
		try ( var loader = serviceLoader() ) {
			for ( int build = 0; build < 2; build++ ) {
				EVENTS.get().clear();
				try ( var factory = buildFactory( entryPoint, loader ) ) {
					assertContributions( factory );
					assertThat( EVENTS.get() ).containsExactly( "dialect", "low", "first-high", "last-high" );
				}
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = EntryPoint.class, names = { "JPA_CONFIGURATION", "HIBERNATE_CONFIGURATION" })
	void jpaPreparationDoesNotInvokeServicesAndMetadataIsReused(EntryPoint entryPoint) throws Exception {
		try ( var loader = serviceLoader() ) {
			final var builder = jpaBuilder( entryPoint, loader );
			try {
				assertThat( EVENTS.get() ).isEmpty();
				final var metadata = builder.metadata();
				assertThat( metadata.getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( TYPE_NAME ) )
						.isSameAs( LAST_HIGH_TYPE );
				assertThat( builder.metadata() ).isSameAs( metadata );
				try ( var factory = builder.build() ) {
					assertContributions( factory );
					assertThat( EVENTS.get() ).containsExactly( "dialect", "low", "first-high", "last-high" );
				}
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@Test
	void applyTypesRemainsImmediateAndExplicitBasicTypesOverrideServices() throws Exception {
		try ( var loader = serviceLoader();
				var bootstrapRegistry = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
				var registry = new StandardServiceRegistryBuilder( bootstrapRegistry ).applySettings( settings( loader ) ).build() ) {
			final var builder = (MetadataBuilderImplementor) new MetadataSources( registry ).getMetadataBuilder();
			builder.applyTypes( explicitContributor( "explicit", EXPLICIT_TYPE ) );
			assertThat( EVENTS.get() ).containsExactly( "explicit" );
			final var metadata = (MetadataImplementor) builder.build();
			assertThat( metadata.getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( TYPE_NAME ) )
					.isSameAs( EXPLICIT_TYPE );
			assertThat( EVENTS.get() ).containsExactly( "explicit", "dialect", "low", "first-high", "last-high" );
		}
	}

	@Test
	void configurationContributorsRetainTheirTimingAndPrecedence() throws Exception {
		try ( var loader = serviceLoader();
				var bootstrapRegistry = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build() ) {
			final var configuration = new Configuration( bootstrapRegistry );
			configuration.getProperties().putAll( settings( loader ) );
			configuration.registerTypeContributor( explicitContributor( "explicit", EXPLICIT_TYPE ) );
			assertThat( EVENTS.get() ).isEmpty();
			try ( var factory = configuration.buildSessionFactory() ) {
				assertThat( typeConfiguration( factory ).getBasicTypeRegistry().getRegisteredType( TYPE_NAME ) )
						.isSameAs( EXPLICIT_TYPE );
				assertThat( EVENTS.get() ).containsExactly( "explicit", "dialect", "low", "first-high", "last-high" );
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = EntryPoint.class, names = { "JPA_CONFIGURATION", "HIBERNATE_CONFIGURATION" })
	void explicitJpaContributorsRetainListOrderAndOverrideServiceBasicTypes(EntryPoint entryPoint) throws Exception {
		try ( var loader = serviceLoader() ) {
			final var configuration = persistenceConfiguration( entryPoint, loader );
			configuration.property( TYPE_CONTRIBUTORS, (TypeContributorList) () -> List.of(
					explicitContributor( "explicit-first", FIRST_HIGH_TYPE, 2000 ),
					explicitContributor( "explicit-last", EXPLICIT_TYPE, 500 )
			) );
			final var builder = jpaBuilder( configuration );
			try {
				assertThat( EVENTS.get() ).containsExactly( "explicit-first", "explicit-last" );
				try ( var factory = builder.build() ) {
					assertThat( typeConfiguration( factory ).getBasicTypeRegistry().getRegisteredType( TYPE_NAME ) )
							.isSameAs( EXPLICIT_TYPE );
					assertThat( EVENTS.get() ).containsExactly(
							"explicit-first", "explicit-last", "dialect", "low", "first-high", "last-high" );
				}
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@Test
	void explicitAndServiceRegistrationsOfTheSameContributorRemainSeparate() throws Exception {
		try ( var loader = serviceLoader() ) {
			final var configuration = persistenceConfiguration( EntryPoint.JPA_CONFIGURATION, loader );
			final var contributor = new LastHighContributor();
			configuration.property( TYPE_CONTRIBUTORS, (TypeContributorList) () -> List.of( contributor, contributor ) );
			final var builder = jpaBuilder( configuration );
			try {
				assertThat( EVENTS.get() ).containsExactly( "last-high", "last-high" );
				try ( var factory = builder.build() ) {
					assertContributions( factory );
					assertThat( EVENTS.get() ).containsExactly(
							"last-high", "last-high", "dialect", "low", "first-high", "last-high" );
				}
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = EntryPoint.class, names = { "JPA_CONFIGURATION", "HIBERNATE_CONFIGURATION" })
	void schemaGenerationUsesSharedContributions(EntryPoint entryPoint) throws Exception {
		try ( var loader = serviceLoader() ) {
			final var script = new StringWriter();
			final var configuration = persistenceConfiguration( entryPoint, loader )
					.property( JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create" )
					.property( JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, script );
			final var builder = jpaBuilder( configuration );
			try {
				builder.generateSchema();
				assertThat( EVENTS.get() ).containsExactly( "dialect", "low", "first-high", "last-high" );
				assertThat( script.toString() ).containsIgnoringCase( "create table ContributionEntity" );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = EntryPoint.class, names = { "JPA_CONFIGURATION", "HIBERNATE_CONFIGURATION" })
	void serviceFailuresOccurDuringMetadataResolution(EntryPoint entryPoint) throws Exception {
		try ( var loader = serviceLoader( FailingContributor.class.getName() ) ) {
			final var builder = jpaBuilder( entryPoint, loader );
			try {
				assertThat( EVENTS.get() ).isEmpty();
				assertThatThrownBy( builder::metadata )
						.isInstanceOf( IllegalStateException.class )
						.hasMessage( "contribution failure" );
				assertThat( EVENTS.get() ).containsExactly( "dialect", "failure" );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	private EntityManagerFactory buildFactory(EntryPoint entryPoint, ClassLoader loader) {
		return switch ( entryPoint ) {
			case METADATA_SOURCES, CONFIGURATION -> {
				final var bootstrapRegistry = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
				if ( entryPoint == EntryPoint.CONFIGURATION ) {
					final var configuration = new Configuration( bootstrapRegistry ).addAnnotatedClass( ContributionEntity.class );
					configuration.getProperties().putAll( settings( loader ) );
					yield configuration.buildSessionFactory();
				}
				final var registry = new StandardServiceRegistryBuilder( bootstrapRegistry ).applySettings( settings( loader ) ).build();
				yield new MetadataSources( registry ).addAnnotatedClass( ContributionEntity.class ).buildMetadata().buildSessionFactory();
			}
			case JPA_CONFIGURATION, HIBERNATE_CONFIGURATION -> new HibernatePersistenceProvider()
					.createEntityManagerFactory( persistenceConfiguration( entryPoint, loader ) );
			case STANDALONE -> new HibernatePersistenceProvider().createEntityManagerFactory( UNIT_NAME, settings( loader ) );
			case CONTAINER -> new HibernatePersistenceProvider().createContainerEntityManagerFactory(
					new PersistenceUnitInfoAdapter() {
						@Override
						public List<String> getManagedClassNames() {
							return List.of( ContributionEntity.class.getName() );
						}
					}, settings( loader ) );
		};
	}

	private static Map<String, Object> settings(ClassLoader loader) {
		return Map.of(
				CLASSLOADERS, List.of( loader ),
				DIALECT, RecordingDialect.class.getName(),
				ALLOW_METADATA_ON_BOOT, false,
				HBM2DDL_AUTO, "none"
		);
	}

	private static PersistenceConfiguration persistenceConfiguration(EntryPoint entryPoint, ClassLoader loader) {
		final PersistenceConfiguration configuration = entryPoint == EntryPoint.HIBERNATE_CONFIGURATION
				? new HibernatePersistenceConfiguration( UNIT_NAME )
				: new PersistenceConfiguration( UNIT_NAME );
		return configuration.properties( settings( loader ) ).managedClass( ContributionEntity.class );
	}

	private static EntityManagerFactoryBuilderImpl jpaBuilder(EntryPoint entryPoint, ClassLoader loader) {
		return jpaBuilder( persistenceConfiguration( entryPoint, loader ) );
	}

	private static EntityManagerFactoryBuilderImpl jpaBuilder(PersistenceConfiguration configuration) {
		return configuration instanceof HibernatePersistenceConfiguration hibernateConfiguration
				? new EntityManagerFactoryBuilderImpl( hibernateConfiguration )
				: new EntityManagerFactoryBuilderImpl( new PersistenceConfigurationDescriptor( configuration ), Map.of() );
	}

	private URLClassLoader serviceLoader() throws IOException {
		// Deliberately reverse ordinal and name order in the service declaration.
		return serviceLoader(
				LastHighContributor.class.getName() + "\n" + FirstHighContributor.class.getName() + "\n"
						+ LowContributor.class.getName() + "\n" );
	}

	private URLClassLoader serviceLoader(String declaration) throws IOException {
		final var services = Files.createDirectories( directory.resolve( "META-INF/services" ) );
		Files.writeString( services.resolve( TypeContributor.class.getName() ), declaration );
		Files.writeString( directory.resolve( "META-INF/persistence.xml" ), """
				<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.2">
					<persistence-unit name="%s" transaction-type="RESOURCE_LOCAL">
						<class>%s</class>
						<exclude-unlisted-classes>true</exclude-unlisted-classes>
					</persistence-unit>
				</persistence>
				""".formatted( UNIT_NAME, ContributionEntity.class.getName() ) );
		return new URLClassLoader( new URL[] { directory.toUri().toURL() }, getClass().getClassLoader() );
	}

	private static void assertContributions(EntityManagerFactory factory) {
		final var types = typeConfiguration( factory );
		assertThat( types.getBasicTypeRegistry().getRegisteredType( TYPE_NAME ) ).isSameAs( LAST_HIGH_TYPE );
		assertThat( types.getJavaTypeRegistry().getDescriptor( String.class ) ).isSameAs( JAVA_TYPE );
		assertThat( types.getJdbcTypeRegistry().getDescriptor( java.sql.Types.VARCHAR ) ).isSameAs( JDBC_TYPE );
		final var entity = factory.unwrap( SessionFactoryImplementor.class ).getMappingMetamodel()
				.getEntityDescriptor( ContributionEntity.class );
		assertThat( entity.getPropertyType( "wrapper" ) ).isInstanceOf( CustomType.class );
		assertThat( entity.getPropertyType( "composite" ) ).isInstanceOf( UserComponentType.class );
		final var converted = (org.hibernate.type.BasicType<?>) entity.getPropertyType( "converted" );
		assertThat( converted.getValueConverter() ).isNotNull();
		assertThat( converted.getValueConverter().getDomainJavaType().getJavaTypeClass() ).isEqualTo( ConvertedValue.class );
	}

	private static TypeConfiguration typeConfiguration(EntityManagerFactory factory) {
		return factory.unwrap( SessionFactoryImplementor.class ).getMappingMetamodel().getTypeConfiguration();
	}

	private static NamedBasicTypeImpl<String> type(String name) {
		return new NamedBasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE, name );
	}

	private static TypeContributor explicitContributor(String event, NamedBasicTypeImpl<String> type) {
		return explicitContributor( event, type, 1000 );
	}

	private static TypeContributor explicitContributor(String event, NamedBasicTypeImpl<String> type, int ordinal) {
		return new TypeContributor() {
			@Override
			public int ordinal() {
				return ordinal;
			}

			@Override
			public void contribute(TypeContributions contributions, ServiceRegistry registry) {
				EVENTS.get().add( event );
				contributions.contributeType( type, TYPE_NAME );
			}
		};
	}

	enum EntryPoint {
		METADATA_SOURCES, CONFIGURATION, JPA_CONFIGURATION, HIBERNATE_CONFIGURATION, STANDALONE, CONTAINER
	}

	public static class RecordingDialect extends H2Dialect {
		@Override
		public void contributeTypes(TypeContributions contributions, ServiceRegistry registry) {
			super.contributeTypes( contributions, registry );
			EVENTS.get().add( "dialect" );
			contributions.contributeType( type( "dialect" ), TYPE_NAME );
		}
	}

	public static class LowContributor implements TypeContributor {
		@Override
		public void contribute(TypeContributions contributions, ServiceRegistry registry) {
			EVENTS.get().add( "low" );
			contributions.contributeJavaType( JAVA_TYPE );
			contributions.contributeJdbcType( JDBC_TYPE );
			contributions.contributeType( LOW_TYPE, TYPE_NAME );
			contributions.contributeType( StringWrapperUserType.INSTANCE );
			contributions.contributeType( MyCompositeValueType.INSTANCE );
			contributions.contributeAttributeConverter( ValueConverter.class );
		}
	}

	public static class FirstHighContributor implements TypeContributor {
		@Override
		public int ordinal() {
			return 2000;
		}

		@Override
		public void contribute(TypeContributions contributions, ServiceRegistry registry) {
			EVENTS.get().add( "first-high" );
			contributions.contributeType( FIRST_HIGH_TYPE, TYPE_NAME );
		}
	}

	public static class LastHighContributor extends FirstHighContributor {
		@Override
		public void contribute(TypeContributions contributions, ServiceRegistry registry) {
			EVENTS.get().add( "last-high" );
			contributions.contributeType( LAST_HIGH_TYPE, TYPE_NAME );
		}
	}

	public static class FailingContributor implements TypeContributor {
		@Override
		public void contribute(TypeContributions contributions, ServiceRegistry registry) {
			EVENTS.get().add( "failure" );
			throw new IllegalStateException( "contribution failure" );
		}
	}

	public record ConvertedValue(String text) {
	}

	@Converter(autoApply = true)
	public static class ValueConverter implements AttributeConverter<ConvertedValue, String> {
		@Override
		public String convertToDatabaseColumn(ConvertedValue attribute) {
			return attribute == null ? null : attribute.text();
		}

		@Override
		public ConvertedValue convertToEntityAttribute(String value) {
			return value == null ? null : new ConvertedValue( value );
		}
	}

	@Entity(name = "ContributionEntity")
	public static class ContributionEntity {
		@Id
		Long id;
		StringWrapper wrapper;
		MyCompositeValue composite;
		ConvertedValue converted;
	}
}
