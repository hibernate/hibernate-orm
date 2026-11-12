/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.io.Serializable;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.java.VersionJavaType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.BasicTypeImpl;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-20757")
@RequiresDialect(H2Dialect.class)
@DomainModel(
		annotatedClasses = MutableVersionDirtyTrackingTest.VersionedEntity.class,
		typeContributors = MutableVersionDirtyTrackingTest.StampTypeContributor.class
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = MutableVersionDirtyTrackingTest.VersionedEntity.class)
class MutableVersionDirtyTrackingTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void loadedEntityRetainsMappedVersionDirtiness(SessionFactoryScope scope) {
		// Insert over JDBC so neither persist nor version seeding can influence the tracker.
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.executeUpdate( "insert into mutable_version_entity (id, version) values (1, 10)" );
			}
		} ) );
		scope.inTransaction( session -> {
			final var entity = session.find( VersionedEntity.class, 1 );
			assertThat( entity.version.value ).isEqualTo( 10 );
			final var persister = scope.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( VersionedEntity.class );
			assertThat( persister.getVersionType().isEqual( entity.version, entity.version ) ).isTrue();
			assertThat( persister.getVersionType().isDirty( entity.version, entity.version, session ) ).isTrue();
			assertThat( session.isDirty() ).isTrue();

			session.flush();

			session.doWork( connection -> {
				try ( var statement = connection.createStatement();
						var results = statement.executeQuery( "select version from mutable_version_entity where id = 1" ) ) {
					assertThat( results.next() ).isTrue();
					assertThat( results.getInt( 1 ) ).isEqualTo( 11 );
					assertThat( results.next() ).isFalse();
				}
			} );
		} );
	}

	@Entity(name = "MutableVersionEntity")
	@Table(name = "mutable_version_entity")
	public static class VersionedEntity {
		@Id
		Integer id;
		@Version
		Stamp version;
	}

	public static class Stamp implements Serializable, Comparable<Stamp> {
		private int value;

		public Stamp(int value) {
			this.value = value;
		}

		@Override
		public int compareTo(Stamp other) {
			return Integer.compare( value, other.value );
		}
	}

	public static class StampTypeContributor implements TypeContributor {
		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			// Version mapping resolves both the Java descriptor and basic type by Java class.
			final var javaType = new StampJavaType();
			typeContributions.contributeJavaType( javaType );
			typeContributions.getTypeConfiguration().getBasicTypeRegistry().register(
					new BasicTypeImpl<>( javaType, IntegerJdbcType.INSTANCE ),
					Stamp.class.getName()
			);
		}
	}

	public static class StampJavaType extends AbstractClassJavaType<Stamp> implements VersionJavaType<Stamp> {
		public StampJavaType() {
			super( Stamp.class, MutableMutabilityPlan.instance() );
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return IntegerJdbcType.INSTANCE;
		}

		@Override
		public boolean areEqual(Stamp one, Stamp another) {
			return one == another || one != null && another != null && one.value == another.value;
		}

		@Override
		public int extractHashCode(Stamp value) {
			return Integer.hashCode( value.value );
		}

		@Override
		public String toString(Stamp value) {
			return value == null ? null : Integer.toString( value.value );
		}

		@Override
		public Stamp fromString(CharSequence value) {
			return value == null ? null : new Stamp( Integer.parseInt( value.toString() ) );
		}

		@Override
		public <X> X unwrap(Stamp value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( type == Integer.class || type == Object.class ) {
				return type.cast( value.value );
			}
			throw unknownUnwrap( type );
		}

		@Override
		public <X> Stamp wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( value instanceof Number number ) {
				return new Stamp( number.intValue() );
			}
			throw unknownWrap( value.getClass() );
		}

		@Override
		public Stamp seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
			return new Stamp( 0 );
		}

		@Override
		public Stamp next(Stamp current, Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
			return new Stamp( current.value + 1 );
		}
	}
}
