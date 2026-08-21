/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.UuidCapableJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				UuidCapableJavaTypeTests.AutoEntity.class,
				UuidCapableJavaTypeTests.ExplicitEntity.class,
				UuidCapableJavaTypeTests.AutoStringEntity.class,
				UuidCapableJavaTypeTests.ExplicitStringEntity.class,
				UuidCapableJavaTypeTests.ExplicitBinaryEntity.class
		},
		typeContributors = UuidCapableJavaTypeTests.CustomUuidTypeContributor.class
)
@SessionFactory
@JiraKey("HHH-20719")
public class UuidCapableJavaTypeTests {
	@Test
	void testAutoGeneration(SessionFactoryScope scope) {
		final var entity = new AutoEntity();

		scope.inTransaction( session -> session.persist( entity ) );

		assertThat( entity.id ).isNotNull();
		assertThat( entity.id.uuid ).isNotNull();
		scope.inTransaction( session -> assertThat( session.find( AutoEntity.class, entity.id ) ).isNotNull() );
	}

	@Test
	void testExplicitGeneration(SessionFactoryScope scope) {
		final var entity = new ExplicitEntity();

		scope.inTransaction( session -> session.persist( entity ) );

		assertThat( entity.id ).isNotNull();
		assertThat( entity.id.uuid ).isNotNull();
		scope.inTransaction( session -> assertThat( session.find( ExplicitEntity.class, entity.id ) ).isNotNull() );
	}

	@Test
	void testBuiltInLegacyTransformerBridges(SessionFactoryScope scope) {
		final var autoString = new AutoStringEntity();
		final var explicitString = new ExplicitStringEntity();
		final var explicitBinary = new ExplicitBinaryEntity();

		scope.inTransaction( session -> {
			session.persist( autoString );
			session.persist( explicitString );
			session.persist( explicitBinary );
		} );

		assertThat( autoString.id ).isNotNull();
		assertThat( UUID.fromString( autoString.id ) ).isNotNull();
		assertThat( explicitString.id ).isNotNull();
		assertThat( UUID.fromString( explicitString.id ) ).isNotNull();
		assertThat( explicitBinary.id ).hasSize( 16 );
	}

	@Entity(name = "AutoUuidCapableEntity")
	@Table(name = "auto_uuid_capable_entity")
	public static class AutoEntity {
		@Id
		@GeneratedValue
		private CustomUuid id;
	}

	@Entity(name = "ExplicitUuidCapableEntity")
	@Table(name = "explicit_uuid_capable_entity")
	public static class ExplicitEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		private CustomUuid id;
	}

	@Entity(name = "AutoStringUuidCapableEntity")
	@Table(name = "auto_string_uuid_capable_entity")
	public static class AutoStringEntity {
		@Id
		@GeneratedValue
		private String id;
	}

	@Entity(name = "ExplicitStringUuidCapableEntity")
	@Table(name = "explicit_string_uuid_capable_entity")
	public static class ExplicitStringEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		private String id;
	}

	@Entity(name = "ExplicitBinaryUuidCapableEntity")
	@Table(name = "explicit_binary_uuid_capable_entity")
	public static class ExplicitBinaryEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		private byte[] id;
	}

	public static final class CustomUuid {
		private final UUID uuid;

		private CustomUuid(UUID uuid) {
			this.uuid = uuid;
		}

		@Override
		public boolean equals(Object object) {
			return this == object
				|| object instanceof CustomUuid that
				&& Objects.equals( uuid, that.uuid );
		}

		@Override
		public int hashCode() {
			return uuid.hashCode();
		}
	}

	public static class CustomUuidJavaType
			extends AbstractClassJavaType<CustomUuid>
			implements UuidCapableJavaType<CustomUuid> {
		public static final CustomUuidJavaType INSTANCE = new CustomUuidJavaType();

		private static final ValueTransformer<CustomUuid> TRANSFORMER = new ValueTransformer<>() {
			@Override
			public CustomUuid transform(UUID uuid) {
				return new CustomUuid( uuid );
			}

			@Override
			public UUID parse(CustomUuid value) {
				return value.uuid;
			}
		};

		public CustomUuidJavaType() {
			super( CustomUuid.class );
		}

		@Override
		public ValueTransformer<CustomUuid> getUuidValueTransformer() {
			return TRANSFORMER;
		}

		@Override
		public boolean prefersUuidGeneration() {
			return true;
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return indicators.getJdbcType( indicators.getPreferredSqlTypeCodeForUuid() );
		}

		@Override
		public String toString(CustomUuid value) {
			return value.uuid.toString();
		}

		@Override
		public CustomUuid fromString(CharSequence string) {
			return new CustomUuid( UUID.fromString( string.toString() ) );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> X unwrap(CustomUuid value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( CustomUuid.class.isAssignableFrom( type ) ) {
				return (X) value;
			}
			if ( UUID.class.isAssignableFrom( type ) ) {
				return (X) value.uuid;
			}
			if ( String.class.isAssignableFrom( type ) ) {
				return (X) value.uuid.toString();
			}
			throw unknownUnwrap( type );
		}

		@Override
		public <X> CustomUuid wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( value instanceof CustomUuid customUuid ) {
				return customUuid;
			}
			if ( value instanceof UUID uuid ) {
				return new CustomUuid( uuid );
			}
			if ( value instanceof String string ) {
				return new CustomUuid( UUID.fromString( string ) );
			}
			throw new HibernateException( "Unknown UUID representation: " + value.getClass().getName() );
		}
	}

	public static class CustomUuidTypeContributor implements TypeContributor {
		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeJavaType( CustomUuidJavaType.INSTANCE );
		}
	}
}
