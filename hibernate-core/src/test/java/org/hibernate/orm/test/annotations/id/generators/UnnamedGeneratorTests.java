/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.generators;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.RootClass;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class UnnamedGeneratorTests {
	@Test
	void testAutoWithNoGenerator() {
		checkSequence( Entity1.class, "Entity1_seq" );
	}

	@Test
	void testAutoWithSequenceGenerator() {
		checkSequence( Entity2.class, "Entity2_seq" );
		checkSequence( Entity3.class, "my_seq" );
		checkSequence( Entity4.class, "another_seq" );
	}

	@Test
	void testAutoWithTableGenerator() {
		checkTableGenerator( Entity5.class, "hibernate_sequences", "Entity5" );
		checkTableGenerator( Entity6.class, "my_sequences", "Entity6" );
		checkTableGenerator( Entity7.class, "sequences_table", "Entity7" );
		checkTableGenerator( Entity8.class, "sequences_table", "ent_8" );
	}

	private void checkSequence(Class<?> entityClass, String expectedDbName) {
		// strictly-global = false
		withGenerator( entityClass, false, (generator) -> {
			final String name = SEQUENCE_NAME_EXTRACTOR.apply( generator );
			assertThat( name ).isEqualToIgnoringCase( expectedDbName );
		} );

		// strictly-global = true
		withGenerator( entityClass, true, (generator) -> {
			final String name = SEQUENCE_NAME_EXTRACTOR.apply( generator );
			assertThat( name ).isEqualToIgnoringCase( expectedDbName );
		} );
	}

	private void checkTableGenerator(Class<?> entityClass, String expectedTableName, String expectedSegmentName) {
		// strictly-global = false
		withGenerator( entityClass, false, (generator) -> {
			final org.hibernate.id.enhanced.TableGenerator tableGenerator = (org.hibernate.id.enhanced.TableGenerator) generator;
			assertThat( tableGenerator.getTableName() ).isEqualToIgnoringCase( expectedTableName );
			assertThat( tableGenerator.getSegmentValue() ).isEqualTo( expectedSegmentName );
		} );

		// strictly-global = true
		withGenerator( entityClass, true, (generator) -> {
			final org.hibernate.id.enhanced.TableGenerator tableGenerator = (org.hibernate.id.enhanced.TableGenerator) generator;
			assertThat( tableGenerator.getTableName() ).isEqualToIgnoringCase( expectedTableName );
			assertThat( tableGenerator.getSegmentValue() ).isEqualTo( expectedSegmentName );
		} );
	}

	private void withGenerator(Class<?> entityClass, boolean strictlyGlobal, Consumer<Generator> checks) {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, strictlyGlobal )
				.build()) {
			final Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClasses( entityClass )
					.buildMetadata();
			final RootClass entityBinding = metadata.getEntityBinding( entityClass.getName() ).getRootClass();
			final Generator generator = entityBinding.getIdentifier().createGenerator(
					metadata.getDatabase().getDialect(),
					entityBinding,
					entityBinding.getIdentifierProperty(),
					new GeneratorSettings() {
						@Override
						public String getDefaultCatalog() {
							return null;
						}

						@Override
						public String getDefaultSchema() {
							return null;
						}

						@Override
						public SqlStringGenerationContext getSqlStringGenerationContext() {
							return SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
						}
					}
			);

			checks.accept( generator );
		}
	}

	@Entity(name="Entity1")
	public static class Entity1 {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
	}

	@Entity(name="Entity2")
	public static class Entity2 {
		@Id
		@GeneratedValue
		@SequenceGenerator
		private Integer id;
		private String name;
	}

	@Entity(name="Entity3")
	public static class Entity3 {
		@Id
		@GeneratedValue
		@SequenceGenerator(sequenceName = "my_seq")
		private Integer id;
		private String name;
	}

	@Entity(name="Entity4")
	@SequenceGenerator(sequenceName = "another_seq")
	public static class Entity4 {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
	}

	@Entity(name="Entity5")
	public static class Entity5 {
		@Id
		@GeneratedValue
		@TableGenerator
		private Integer id;
		private String name;
	}

	@Entity(name="Entity6")
	public static class Entity6 {
		@Id
		@GeneratedValue
		@TableGenerator(table = "my_sequences")
		private Integer id;
		private String name;
	}

	@Entity(name="Entity7")
	@TableGenerator(table = "sequences_table")
	public static class Entity7 {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
	}

	@Entity(name="Entity8")
	@TableGenerator(table = "sequences_table", pkColumnValue = "ent_8")
	public static class Entity8 {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
	}

	private static final Function<Generator,String> SEQUENCE_NAME_EXTRACTOR = (generator) -> {
		return ( (org.hibernate.id.enhanced.SequenceStyleGenerator) generator ).getDatabaseStructure().getPhysicalName().getObjectName().getText();
	};
}
