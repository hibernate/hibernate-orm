/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.enhanced;

import java.util.function.Consumer;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.id.enhanced.StandardNamingStrategy;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.orm.test.idgen.n_ative.GeneratorSettingsImpl;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.GenerationType.TABLE;
import static org.assertj.core.api.Assertions.assertThat;

@BaseUnitTest
public class TableNamingStrategyTest {
	@Test
	public void testTableNameStandardStrategy() {
		verifyStandardStrategy( TestEntity.class, "hibernate_sequences" );
	}

	@Test
	public void testNoGeneratorStandardStrategy() {
		verifyStandardStrategy( TestEntity2.class, "table_generator" );
	}

	@Test
	public void testNoGeneratorNoPreferGeneratorNameStrategy() {
		verifyLegacyStrategy( TestEntity2.class, "hibernate_sequences" );
	}

	@Test
	public void testNoGeneratorPreferGeneratorNameStrategy() {
		verifyLegacyPreferStrategy( TestEntity2.class, "table_generator" );
	}

	@Test
	public void testGeneratorWithoutSequenceNameStandardStrategy() {
		verifyStandardStrategy( TestEntity3.class, "table_generator" );
	}

	@Test
	public void testGeneratorWithoutSequenceNameStandardStrategyNoPreferGeneratorNameStrategy() {
		verifyLegacyStrategy( TestEntity3.class, "hibernate_sequences" );
	}

	@Test
	public void testGeneratorWithoutSequenceNameStandardStrategyPreferGeneratorNameStrategy() {
		verifyLegacyPreferStrategy( TestEntity3.class, "table_generator" );
	}

	@Test
	public void testGeneratorWithSequenceNameStandardStrategy() {
		verifyStandardStrategy( TestEntity4.class, "test_table" );
	}

	@Test
	public void testGeneratorWithSequenceNameNoPreferGeneratorNameStrategy() {
		verifyLegacyStrategy( TestEntity4.class, "test_table" );
	}

	@Test
	public void testGeneratorWithSequenceNamePreferGeneratorNameStrategy() {
		verifyLegacyPreferStrategy( TestEntity4.class, "test_table" );
	}

	private void verifyStandardStrategy(Class<?> entityClass, String expectedName) {
		verify( entityClass, expectedName );
		verify( entityClass, StandardNamingStrategy.STRATEGY_NAME, expectedName );
		verify( entityClass, StandardNamingStrategy.class.getName(), expectedName );
	}

	private void verifyLegacyStrategy(Class<?> entityClass, String expectedName) {
		verify( entityClass, SingleNamingStrategy.STRATEGY_NAME, expectedName );
		verify( entityClass, SingleNamingStrategy.class.getName(), expectedName );
	}

	private void verifyLegacyPreferStrategy(Class<?> entityClass, String expectedName) {
		verify( entityClass, LegacyNamingStrategy.STRATEGY_NAME, expectedName );
		verify( entityClass, LegacyNamingStrategy.class.getName(), expectedName );
	}

	private void verify(Class<?> entityType, String expectedName) {
		verify( entityType, null, expectedName );
	}

	private void verify(Class<?> entityType, String strategy, String expectedName) {
		withMetadata( entityType, strategy, (metadata) -> {
			final Namespace defaultNamespace = metadata.getDatabase().getDefaultNamespace();
			final Table table = defaultNamespace.locateTable( Identifier.toIdentifier( expectedName ) );
			assertThat( table ).isNotNull();

			final PersistentClass entityBinding = metadata.getEntityBinding( entityType.getName() );
			final IdentifierGenerator generator = extractGenerator( entityBinding, metadata );
			assertThat( generator ).isInstanceOf( org.hibernate.id.enhanced.TableGenerator.class );
			final org.hibernate.id.enhanced.TableGenerator tableGenerator = (org.hibernate.id.enhanced.TableGenerator) generator;
			assertThat( tableGenerator.getTableName() ).isEqualTo( expectedName );
		} );
	}

	private static void withMetadata(Class<?> entityClass, String namingStrategy, Consumer<MetadataImplementor> consumer) {
		final StandardServiceRegistryBuilder ssrb = ServiceRegistryUtil.serviceRegistryBuilder();
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );

		if ( namingStrategy != null ) {
			ssrb.applySetting( AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY, namingStrategy );
		}

		try ( final ServiceRegistry ssr = ssrb.build() ) {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( entityClass );

			final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			consumer.accept( metadata );
		}
	}

	private IdentifierGenerator extractGenerator(PersistentClass entityBinding, MetadataImplementor metadata) {
		KeyValue keyValue = entityBinding.getIdentifier();
		final Generator generator = keyValue.createGenerator(
				metadata.getDatabase().getDialect(),
				entityBinding.getRootClass(),
				entityBinding.getIdentifierProperty(),
				new GeneratorSettingsImpl( metadata )
		);
		return generator instanceof IdentifierGenerator ? (IdentifierGenerator) generator : null;
	}


	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue(strategy = TABLE)
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity2")
	public static class TestEntity2 {
		@Id
		@GeneratedValue(strategy = TABLE, generator = "table_generator")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity3")
	public static class TestEntity3 {
		@Id
		@GeneratedValue(generator = "table_generator")
		@TableGenerator(name = "table_generator")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity4")
	public static class TestEntity4 {
		@Id
		@GeneratedValue(generator = "table_generator")
		@TableGenerator(name = "table_generator", table = "test_table")
		private Long id;

		private String name;
	}

}
