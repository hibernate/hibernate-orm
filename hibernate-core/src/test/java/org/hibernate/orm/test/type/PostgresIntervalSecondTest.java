/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.PostgreSQLIntervalSecondJdbcType;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.NumericJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Test to see if using `@org.hibernate.annotations.JdbcTypeCode` or `@org.hibernate.annotations.JdbcType`
 * will override a default JdbcType set by a {@link AvailableSettings#PREFERRED_DURATION_JDBC_TYPE config property}.
 */
@SessionFactory
@DomainModel(annotatedClasses = { PostgresIntervalSecondTest.EntityWithIntervalSecondDuration.class })
@ServiceRegistry(settings = @Setting(name = AvailableSettings.PREFERRED_DURATION_JDBC_TYPE, value = "NUMERIC"))
@RequiresDialect(PostgreSQLDialect.class)
public class PostgresIntervalSecondTest {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(
				PostgresIntervalSecondTest.EntityWithIntervalSecondDuration.class );
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final JdbcType durationJdbcType = mappingMetamodel.getTypeConfiguration()
				.getBasicTypeForJavaType( Duration.class )
				.getJdbcType();

		// default interval type set by a config property and should be `NUMERIC`
		assertThat( durationJdbcType ).isEqualTo( NumericJdbcType.INSTANCE );

		final JdbcType intervalType = jdbcTypeRegistry.getDescriptor( SqlTypes.INTERVAL_SECOND );
		assertThat( intervalType ).isOfAnyClassIn( PostgreSQLIntervalSecondJdbcType.class );

		// a simple duration field with no overrides - so should be using a default JdbcType
		assertThat( entityDescriptor.findAttributeMapping( "duration" )
				.getSingleJdbcMapping().getJdbcType() )
				.isEqualTo( durationJdbcType );

		// a field that is using a @JdbcType annotation to override the JdbcType. Hence, the used JdbcType must match the one
		// set by the annotation.
		assertThat( entityDescriptor.findAttributeMapping( "durationJdbcType" )
				.getSingleJdbcMapping().getJdbcType() )
				.isNotEqualTo( durationJdbcType )
				.isOfAnyClassIn( PostgreSQLIntervalSecondJdbcType.class );

		// a field that is using a @JdbcTypeCode annotation to override the JdbcType. Hence, the used JdbcType must match the one
		// set by the annotation.
		assertThat( entityDescriptor.findAttributeMapping( "durationJdbcTypeCode" )
				.getSingleJdbcMapping().getJdbcType() )
				.isEqualTo( intervalType );
	}

	@Entity(name = "EntityWithIntervalSecondDuration")
	@Table(name = "EntityWithIntervalSecondDuration")
	public static class EntityWithIntervalSecondDuration {
		@Id
		private Integer id;

		private Duration duration;

		@Column(precision = 10, scale = 6)
		@JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
		private Duration durationJdbcTypeCode;

		@Column(precision = 10, scale = 6)
		@org.hibernate.annotations.JdbcType(PostgreSQLIntervalSecondJdbcType.class)
		private Duration durationJdbcType;

		public EntityWithIntervalSecondDuration() {
		}
	}
}
