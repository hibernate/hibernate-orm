/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * Tests of various aspects of {@link GeneratedValue} handling in regards to determining
 * the {@link IdentifierGenerator} to use
 *
 * @author Steve Ebersole
 */
@RequiresDialect( value = H2Dialect.class, comment = "Really, these tests are independent of the underlying database - but Dialects that do not support sequences cause some assertions to erroneously fail" )
public class GeneratedValueTests extends BaseUnitTestCase {
	@Test
	public void baseline() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ExplicitGeneratorEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding( ExplicitGeneratorEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			generator.initialize( SqlStringGenerationContextImpl.forTests( bootModel.getDatabase().getJdbcEnvironment() ) );

			final SequenceStyleGenerator sequenceStyleGenerator = assertTyping(
					SequenceStyleGenerator.class,
					generator
			);
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getPhysicalName().render(), is( "my_real_db_sequence" ) );

			// all the JPA defaults since they were not defined
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 100 ) );
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 500 ) );
		}
	}

	@Test
	public void testImplicitSequenceGenerator() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ImplicitSequenceGeneratorEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding( ImplicitSequenceGeneratorEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			generator.initialize( SqlStringGenerationContextImpl.forTests( bootModel.getDatabase().getJdbcEnvironment() ) );

			final SequenceStyleGenerator sequenceStyleGenerator = assertTyping(
					SequenceStyleGenerator.class,
					generator
			);

			// PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME == false indicates that the legacy
			// 		default (hibernate_sequence) should be used
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getPhysicalName().render(), is( "my_db_sequence" ) );

			// the JPA defaults since they were not defined
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 1 ) );
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 50 ) );
		}
	}

	@Test
	public void testImplicitSequenceGeneratorGeneratorName() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ImplicitSequenceGeneratorEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding( ImplicitSequenceGeneratorEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			generator.initialize( SqlStringGenerationContextImpl.forTests( bootModel.getDatabase().getJdbcEnvironment() ) );

			final SequenceStyleGenerator sequenceStyleGenerator = assertTyping(
					SequenceStyleGenerator.class,
					generator
			);

			// PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME == true (the default) indicates that the generator-name
			//		should be used as the default instead.
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getPhysicalName().render(), is( "my_db_sequence" ) );

			// the JPA defaults since they were not defined
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 1 ) );
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 50 ) );
		}
	}

	@Test
	public void testExplicitSequenceGeneratorImplicitNamePreferGeneratorName() {
		// this should be the default behavior
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ExplicitSequenceGeneratorImplicitNameEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding(
					ExplicitSequenceGeneratorImplicitNameEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			Database database = bootModel.getDatabase();
			SqlStringGenerationContext sqlStringGenerationContext =
					SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
			generator.initialize( sqlStringGenerationContext );

			final SequenceStyleGenerator sequenceStyleGenerator = assertTyping(
					SequenceStyleGenerator.class,
					generator
			);
			// all the JPA defaults since they were not defined
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getPhysicalName().render(), is( "my_db_sequence" ) );
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getInitialValue(), is( 100 ) );
			assertThat( sequenceStyleGenerator.getDatabaseStructure().getIncrementSize(), is( 500 ) );

			final Sequence sequence = database.getDefaultNamespace()
					.locateSequence( Identifier.toIdentifier( "my_db_sequence" ) );
			assertThat( sequence, notNullValue() );
			assertThat( sequence.getName().getSequenceName().getText(), is( "my_db_sequence" ) );
			assertThat( sequence.getInitialValue(), is( 100 ) );
			assertThat( sequence.getIncrementSize(), is( 500 ) );

			final String[] sqlCreateStrings = new H2Dialect().getSequenceExporter().getSqlCreateStrings(
					sequence,
					bootModel,
					sqlStringGenerationContext
			);
			assertThat( sqlCreateStrings.length, is( 1 ) );
			final String cmd = sqlCreateStrings[0].toLowerCase();
			assertTrue( cmd.startsWith( "create sequence my_db_sequence start with 100 increment by 500" ) );
		}
	}

	@Test
	public void testImplicitTableGenerator() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ImplicitTableGeneratorEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding( ImplicitTableGeneratorEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			generator.initialize( SqlStringGenerationContextImpl.forTests( bootModel.getDatabase().getJdbcEnvironment() ) );

			final TableGenerator tableGenerator = assertTyping( TableGenerator.class, generator );

			assertThat( tableGenerator.getTableName(), is( "my_id_table" ) );

			// all the JPA defaults since they were not defined
			assertThat( tableGenerator.getInitialValue(), is( 1 ) );
			assertThat( tableGenerator.getIncrementSize(), is( 50 ) );
		}
	}

	@Test
	public void testExplicitTableGeneratorImplicitName() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ExplicitTableGeneratorImplicitNameEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding( ExplicitTableGeneratorImplicitNameEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			generator.initialize( SqlStringGenerationContextImpl.forTests( bootModel.getDatabase().getJdbcEnvironment() ) );

			final TableGenerator tableGenerator = assertTyping( TableGenerator.class, generator );

			assertThat( tableGenerator.getTableName(), is( "my_id_table" ) );

			//		- note : currently initialValue=1 in mapping is shows up here as 2
			assertThat( tableGenerator.getInitialValue(), is( 1 ) );
			assertThat( tableGenerator.getIncrementSize(), is( 25 ) );
		}
	}

	@Test
	public void testExplicitTableGenerator() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ExplicitTableGeneratorEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding( ExplicitTableGeneratorEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			generator.initialize( SqlStringGenerationContextImpl.forTests( bootModel.getDatabase().getJdbcEnvironment() ) );

			final TableGenerator tableGenerator = assertTyping( TableGenerator.class, generator );

			assertThat( tableGenerator.getTableName(), is( "my_real_id_table" ) );

			// all the JPA defaults since they were not defined
			//		- note : currently initialValue=1 in mapping is shows up here
			//			as 2
//		assertThat( tableGenerator.getInitialValue(), is( 1 ) );
			assertThat( tableGenerator.getIncrementSize(), is( 25 ) );
		}
	}

	@Test
	public void testExplicitIncrementGenerator() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ExplicitIncrementGeneratorEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding( ExplicitIncrementGeneratorEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			generator.initialize( SqlStringGenerationContextImpl.forTests( bootModel.getDatabase().getJdbcEnvironment() ) );

			assertTyping( IncrementGenerator.class, generator );
		}
	}

	@Test
	public void testImplicitIncrementGenerator() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataImplementor bootModel = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( ImplicitIncrementGeneratorEntity.class )
					.buildMetadata();
			final PersistentClass entityMapping = bootModel.getEntityBinding( ImplicitIncrementGeneratorEntity.class.getName() );
			KeyValue keyValue = entityMapping.getIdentifier();
			Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
			final Generator generator1 = keyValue.createGenerator( dialect, (RootClass) entityMapping);
			final IdentifierGenerator generator = generator1 instanceof IdentifierGenerator ? (IdentifierGenerator) generator1 : null;
			generator.initialize( SqlStringGenerationContextImpl.forTests( bootModel.getDatabase().getJdbcEnvironment() ) );

			assertTyping( IncrementGenerator.class, generator );
		}
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
		 * This entity does have explicit {@link SequenceGenerator} defined
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
		 * This entity does not have explicit {@link jakarta.persistence.TableGenerator} defined
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.TABLE, generator = "my_id_table" )
		public Integer id;
		public String name;
	}

	@Entity
	public static class ExplicitTableGeneratorImplicitNameEntity {
		/**
		 * This entity has an explicit {@link jakarta.persistence.TableGenerator} defined,
		 * but does not define {@link jakarta.persistence.TableGenerator#table()}.  In
		 * this case, the generator-name ("my_id_table")
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.TABLE, generator = "my_id_table" )
		@jakarta.persistence.TableGenerator( name = "my_id_table", allocationSize = 25 )
		public Integer id;
		public String name;
	}

	@Entity
	@jakarta.persistence.TableGenerator(
			name = "my_id_table",
			table = "my_real_id_table",
			pkColumnName = "PK_COL",
			valueColumnName = "VAL_COL",
			pkColumnValue = "DT1_ID",
			allocationSize = 25
	)
	public static class ExplicitTableGeneratorEntity {
		/**
		 * This entity has an explicit {@link jakarta.persistence.TableGenerator} defined,
		 * and specifies a table name.  That table name ("my_real_id_table") should be used.
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.TABLE, generator = "my_id_table" )
		public Integer id;
		public String name;
	}

	@Entity
	public static class ExplicitIncrementGeneratorEntity {
		/**
		 * This entity does not have explicit {@link jakarta.persistence.TableGenerator} defined
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
		 * This entity does not have explicit {@link jakarta.persistence.TableGenerator} defined
		 */
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO, generator = "increment" )
		public Integer id;
		public String name;
	}
}
