package org.hibernate.orm.test.idgen.userdefined;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.stream.Stream;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;

@JiraKey("HHH-21211")
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "create-drop"))
class IgnoredGeneratedValueGeneratorTest {

	@RegisterExtension
	final LoggerInspectionExtension logger = LoggerInspectionExtension.builder().setLogger( BOOT_LOGGER ).build();

	@ParameterizedTest
	@MethodSource("ignoredNames")
	void warnsWithoutChangingGeneration(Class<?> entityType, Class<?> annotationType, ServiceRegistryScope scope)
			throws Exception {
		final var warning = logger.watchForLogMessages( "HHH160249" );

		assertThat( persist( entityType, scope ) ).isEqualTo( 42L );
		assertThat( warning.triggerMessages() ).singleElement().asString()
				.contains( entityType.getName() + ".id", "ignored", annotationType.getName() );
	}

	static Stream<Arguments> ignoredNames() {
		return Stream.of(
				Arguments.of( FieldEntity.class, GenericGenerator.class ),
				Arguments.of( PropertyEntity.class, GenericGenerator.class ),
				Arguments.of( CustomEntity.class, FixedId.class ),
				Arguments.of( OverriddenNamedEntity.class, GenericGenerator.class )
		);
	}

	@ParameterizedTest
	@MethodSource("validMappings")
	void doesNotWarnForValidMappings(Class<?> entityType, long expectedId, ServiceRegistryScope scope) throws Exception {
		final var warning = logger.watchForLogMessages( "HHH160249" );

		assertThat( persist( entityType, scope ) ).isEqualTo( expectedId );
		assertThat( warning.triggerMessages() ).isEmpty();
	}

	static Stream<Arguments> validMappings() {
		return Stream.of(
				Arguments.of( BareGeneratedValueEntity.class, 42L ),
				Arguments.of( NoGeneratedValueEntity.class, 42L ),
				Arguments.of( ClassLevelGeneratorEntity.class, 42L ),
				Arguments.of( NamedSequenceEntity.class, 1L ),
				Arguments.of( NamedTableEntity.class, 1L )
		);
	}

	private Long persist(Class<?> entityType, ServiceRegistryScope scope) throws Exception {
		try (var factory = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( entityType ).buildMetadata().buildSessionFactory()) {
			final var entity = entityType.getDeclaredConstructor().newInstance();
			return factory.fromTransaction( session -> {
				session.persist( entity );
				return (Long) session.getIdentifier( entity );
			} );
		}
	}

	public static class FixedGenerator implements IdentifierGenerator {
		@Override
		public Object generate(SharedSessionContractImplementor session, Object object) {
			return 42L;
		}
	}

	@IdGeneratorType(FixedGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	@interface FixedId {
	}

	@Entity(name = "FieldEntity")
	public static class FieldEntity {
		@Id
		@GeneratedValue(generator = "ignored")
		@GenericGenerator(type = FixedGenerator.class)
		Long id;
	}

	@Entity(name = "PropertyEntity")
	public static class PropertyEntity {
		private Long id;

		@Id
		@GeneratedValue(generator = "ignored")
		@GenericGenerator(type = FixedGenerator.class)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "CustomEntity")
	public static class CustomEntity {
		@Id
		@GeneratedValue(generator = "ignored")
		@FixedId
		Long id;
	}

	@Entity(name = "OverriddenNamedEntity")
	public static class OverriddenNamedEntity {
		@Id
		@GeneratedValue(generator = "ignored")
		@SequenceGenerator(name = "ignored", sequenceName = "ignored_sequence", initialValue = 100)
		@GenericGenerator(type = FixedGenerator.class)
		Long id;
	}

	@Entity(name = "BareGeneratedValueEntity")
	public static class BareGeneratedValueEntity {
		@Id
		@GeneratedValue
		@GenericGenerator(type = FixedGenerator.class)
		Long id;
	}

	@Entity(name = "NoGeneratedValueEntity")
	public static class NoGeneratedValueEntity {
		@Id
		@GenericGenerator(type = FixedGenerator.class)
		Long id;
	}

	@Entity(name = "ClassLevelGeneratorEntity")
	@GenericGenerator(type = FixedGenerator.class)
	public static class ClassLevelGeneratorEntity {
		@Id
		@GeneratedValue
		Long id;
	}

	@Entity(name = "NamedSequenceEntity")
	public static class NamedSequenceEntity {
		@Id
		@GeneratedValue(generator = "named_sequence")
		@SequenceGenerator(name = "named_sequence", sequenceName = "test_sequence", allocationSize = 1)
		Long id;
	}

	@Entity(name = "NamedTableEntity")
	public static class NamedTableEntity {
		@Id
		@GeneratedValue(generator = "named_table")
		@TableGenerator(name = "named_table", table = "id_table", allocationSize = 1)
		Long id;
	}
}
