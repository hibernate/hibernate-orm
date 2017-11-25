/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * Tests of various aspects of {@link GeneratedValue} handling in regards to determining
 * the {@link IdentifierGenerator} to use
 *
 * @author Steve Ebersole
 */
public class GeneratedValueTests extends BaseUnitTestCase {
	@Test
	public void baseline() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		final Metadata bootModel = new MetadataSources( ssr )
				.addAnnotatedClass( ExplicitGeneratorEntity.class )
				.buildMetadata();
		final PersistentClass entityMapping = bootModel.getEntityBinding( ExplicitGeneratorEntity.class.getName() );
		final IdentifierGenerator generator  = entityMapping.getIdentifier().createIdentifierGenerator(
				bootModel.getIdentifierGeneratorFactory(),
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				null,
				null,
				(RootClass) entityMapping
		);

		final SequenceStyleGenerator sequenceStyleGenerator = assertTyping( SequenceStyleGenerator.class, generator );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getName(), is( "my_real_db_sequence" ) );
		// all the JPA defaults since they were not defined
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 100 ) );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 500 ) );
	}

	@Test
	public void testImplicitSequenceGenerator() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME, "false" )
				.build();
		final Metadata bootModel = new MetadataSources( ssr )
				.addAnnotatedClass( ImplicitSequenceGeneratorEntity.class )
				.buildMetadata();
		final PersistentClass entityMapping = bootModel.getEntityBinding( ImplicitSequenceGeneratorEntity.class.getName() );
		final IdentifierGenerator generator  = entityMapping.getIdentifier().createIdentifierGenerator(
				bootModel.getIdentifierGeneratorFactory(),
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				null,
				null,
				(RootClass) entityMapping
		);

		final SequenceStyleGenerator sequenceStyleGenerator = assertTyping( SequenceStyleGenerator.class, generator );
		// all the JPA defaults since they were not defined
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 1 ) );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 50 ) );
	}

	@Test
	public void testImplicitSequenceGeneratorGeneratorName() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		final Metadata bootModel = new MetadataSources( ssr )
				.addAnnotatedClass( ImplicitSequenceGeneratorEntity.class )
				.buildMetadata();
		final PersistentClass entityMapping = bootModel.getEntityBinding( ImplicitSequenceGeneratorEntity.class.getName() );
		final IdentifierGenerator generator  = entityMapping.getIdentifier().createIdentifierGenerator(
				bootModel.getIdentifierGeneratorFactory(),
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				null,
				null,
				(RootClass) entityMapping
		);

		final SequenceStyleGenerator sequenceStyleGenerator = assertTyping( SequenceStyleGenerator.class, generator );
		// all the JPA defaults since they were not defined
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getName(), is( "my_db_sequence" ) );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 1 ) );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 50 ) );
	}

	@Test
	public void testExplicitSequenceGeneratorImplicitName() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME, "false" )
				.build();
		final Metadata bootModel = new MetadataSources( ssr )
				.addAnnotatedClass( ExplicitSequenceGeneratorImplicitNameEntity.class )
				.buildMetadata();
		final PersistentClass entityMapping = bootModel.getEntityBinding( ExplicitSequenceGeneratorImplicitNameEntity.class.getName() );
		final IdentifierGenerator generator  = entityMapping.getIdentifier().createIdentifierGenerator(
				bootModel.getIdentifierGeneratorFactory(),
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				null,
				null,
				(RootClass) entityMapping
		);

		final SequenceStyleGenerator sequenceStyleGenerator = assertTyping( SequenceStyleGenerator.class, generator );
		// all the JPA defaults since they were not defined
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getName(), is( SequenceStyleGenerator.DEF_SEQUENCE_NAME ) );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 100 ) );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 500 ) );
	}

	@Test
	public void testExplicitSequenceGeneratorImplicitNamePreferGeneratorName() {
		// this should be the default behavior
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		final Metadata bootModel = new MetadataSources( ssr )
				.addAnnotatedClass( ExplicitSequenceGeneratorImplicitNameEntity.class )
				.buildMetadata();
		final PersistentClass entityMapping = bootModel.getEntityBinding( ExplicitSequenceGeneratorImplicitNameEntity.class.getName() );
		final IdentifierGenerator generator  = entityMapping.getIdentifier().createIdentifierGenerator(
				bootModel.getIdentifierGeneratorFactory(),
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				null,
				null,
				(RootClass) entityMapping
		);

		final SequenceStyleGenerator sequenceStyleGenerator = assertTyping( SequenceStyleGenerator.class, generator );
		// all the JPA defaults since they were not defined
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getName(), is( "my_db_sequence" ) );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 100 ) );
		assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 500 ) );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12122", message = "for some reason the initial value here gets interpreted as 2; other than that this works" )
	public void testImplicitTableGenerator() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		final Metadata bootModel = new MetadataSources( ssr )
				.addAnnotatedClass( ImplicitTableGeneratorEntity.class )
				.buildMetadata();
		final PersistentClass entityMapping = bootModel.getEntityBinding( ImplicitTableGeneratorEntity.class.getName() );
		final IdentifierGenerator generator  = entityMapping.getIdentifier().createIdentifierGenerator(
				bootModel.getIdentifierGeneratorFactory(),
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				null,
				null,
				(RootClass) entityMapping
		);

		final TableGenerator tableGenerator = assertTyping( TableGenerator.class, generator );
		// all the JPA defaults since they were not defined
		assertThat( tableGenerator.getInitialValue(), is( 1 ) );
		assertThat( tableGenerator.getIncrementSize(), is( 50 ) );
	}

	@Test
	public void testExplicitIncrementGenerator() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		final Metadata bootModel = new MetadataSources( ssr )
				.addAnnotatedClass( ExplicitIncrementGeneratorEntity.class )
				.buildMetadata();
		final PersistentClass entityMapping = bootModel.getEntityBinding( ExplicitIncrementGeneratorEntity.class.getName() );
		final IdentifierGenerator generator  = entityMapping.getIdentifier().createIdentifierGenerator(
				bootModel.getIdentifierGeneratorFactory(),
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				null,
				null,
				(RootClass) entityMapping
		);

		assertTyping( IncrementGenerator.class, generator );
	}

	@Test
	public void testImplicitIncrementGenerator() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		final Metadata bootModel = new MetadataSources( ssr )
				.addAnnotatedClass( ImplicitIncrementGeneratorEntity.class )
				.buildMetadata();
		final PersistentClass entityMapping = bootModel.getEntityBinding( ImplicitIncrementGeneratorEntity.class.getName() );
		final IdentifierGenerator generator  = entityMapping.getIdentifier().createIdentifierGenerator(
				bootModel.getIdentifierGeneratorFactory(),
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				null,
				null,
				(RootClass) entityMapping
		);

		assertTyping( IncrementGenerator.class, generator );
	}

	@Entity
	public static class ExplicitGeneratorEntity {
		/**
		 * This entity has an explicit {@link SequenceGenerator} defined
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "my_db_sequence" )
		@SequenceGenerator( name = "my_db_sequence", sequenceName = "my_real_db_sequence", initialValue = 100, allocationSize = 500 )
		public Integer id;
		public String name;
	}

	@Entity
	public static class ImplicitSequenceGeneratorEntity {
		/**
		 * This entity does not have explicit {@link SequenceGenerator} defined
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "my_db_sequence" )
		public Integer id;
		public String name;
	}

	@Entity
	public static class ExplicitSequenceGeneratorImplicitNameEntity {
		/**
		 * This entity does not have explicit {@link SequenceGenerator} defined
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "my_db_sequence" )
		@SequenceGenerator( name = "my_db_sequence", initialValue = 100, allocationSize = 500 )
		public Integer id;
		public String name;
	}

	@Entity
	public static class ImplicitTableGeneratorEntity {
		/**
		 * This entity does not have explicit {@link javax.persistence.TableGenerator} defined
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.TABLE, generator = "my_id_table" )
		public Integer id;
		public String name;
	}

	@Entity
	public static class ExplicitIncrementGeneratorEntity {
		/**
		 * This entity does not have explicit {@link javax.persistence.TableGenerator} defined
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO, generator = "increment" )
		@GenericGenerator( name = "increment", strategy = "increment" )
		public Integer id;
		public String name;
	}

	@Entity
	public static class ImplicitIncrementGeneratorEntity {
		/**
		 * This entity does not have explicit {@link javax.persistence.TableGenerator} defined
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO, generator = "increment" )
		public Integer id;
		public String name;
	}
}
