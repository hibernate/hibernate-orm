/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.descriptor;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.BasicValue;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(xmlMappings = "org/hibernate/orm/test/idgen/descriptor/legacy-generator.hbm.xml")
@SessionFactory
class LegacyGeneratorDescriptorTests {
	@Test
	void legacyStrategyIsClassifiedAndPreparedOnce(SessionFactoryScope scope) {
		final var entityBinding =
				scope.getMetadataImplementor().getEntityBinding( LegacyEntity.class.getName() );
		final var descriptor =
				((BasicValue) entityBinding.getIdentifier()).getCustomIdGeneratorCreator();

		assertThat( descriptor.getGeneratorClass( null ) )
				.isEqualTo( LegacyExportableGenerator.class );
		assertThat( LegacyExportableGenerator.CONSTRUCTION_COUNT ).hasValue( 1 );
		assertThat( LegacyExportableGenerator.CONFIGURED_VALUE ).isEqualTo( 17L );
		assertThat( LegacyExportableGenerator.REGISTERED_INSTANCE ).isNotNull();

		final var entity = new LegacyEntity();
		scope.inTransaction( session -> session.persist( entity ) );

		assertThat( entity.getId() ).isEqualTo( 17L );
		assertThat( LegacyExportableGenerator.CONSTRUCTION_COUNT ).hasValue( 1 );
		assertThat( LegacyExportableGenerator.GENERATING_INSTANCE )
				.isSameAs( LegacyExportableGenerator.REGISTERED_INSTANCE );
	}

	public static class LegacyExportableGenerator implements IdentifierGenerator, ExportableProducer {
		private static final AtomicInteger CONSTRUCTION_COUNT = new AtomicInteger();
		private static long CONFIGURED_VALUE;
		private static LegacyExportableGenerator REGISTERED_INSTANCE;
		private static LegacyExportableGenerator GENERATING_INSTANCE;

		public LegacyExportableGenerator() {
			CONSTRUCTION_COUNT.incrementAndGet();
		}

		@Override
		public void configure(GeneratorCreationContext creationContext, Properties parameters) {
			CONFIGURED_VALUE = Long.parseLong( parameters.getProperty( "configured-value" ) );
		}

		@Override
		public void registerExportables(Database database) {
			REGISTERED_INSTANCE = this;
		}

		@Override
		public Object generate(SharedSessionContractImplementor session, Object object) {
			GENERATING_INSTANCE = this;
			return CONFIGURED_VALUE;
		}
	}

	public static class LegacyEntity {
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
