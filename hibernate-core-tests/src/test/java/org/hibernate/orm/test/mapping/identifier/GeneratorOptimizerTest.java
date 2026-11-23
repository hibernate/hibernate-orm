package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import org.hibernate.annotations.GeneratorOptimizer;
import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.hibernate.id.enhanced.PooledLoThreadLocalOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE;

/// User Guide examples of optimizer selection for local and global generator definitions.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		GeneratorOptimizerTest.LocalSequenceEntity.class,
		GeneratorOptimizerTest.LocalTableEntity.class,
		GeneratorOptimizerTest.GeneratorDefinitions.class,
		GeneratorOptimizerTest.FirstConsumer.class,
		GeneratorOptimizerTest.SecondConsumer.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, value = "true"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportPooledSequences.class)
public class GeneratorOptimizerTest {
	@Test
	public void testLocalGenerators(SessionFactoryScope scope) {
		final var metamodel = scope.getSessionFactory().getMappingMetamodel();
		final var sequenceGenerator = assertInstanceOf( SequenceStyleGenerator.class,
				metamodel.getEntityDescriptor( LocalSequenceEntity.class ).getGenerator() );
		assertInstanceOf( PooledLoOptimizer.class, sequenceGenerator.getOptimizer() );
		assertEquals( 20, sequenceGenerator.getOptimizer().getIncrementSize() );

		final var tableGenerator = assertInstanceOf( org.hibernate.id.enhanced.TableGenerator.class,
				metamodel.getEntityDescriptor( LocalTableEntity.class ).getGenerator() );
		assertInstanceOf( PooledLoOptimizer.class, tableGenerator.getOptimizer() );
		assertEquals( 20, tableGenerator.getOptimizer().getIncrementSize() );

		scope.inTransaction( session -> {
			final var sequenceEntity = new LocalSequenceEntity();
			final var tableEntity = new LocalTableEntity();
			session.persist( sequenceEntity );
			session.persist( tableEntity );
			assertNotNull( sequenceEntity.id );
			assertNotNull( tableEntity.id );
		} );
	}

	@Test
	public void testGlobalGeneratorConsumers(SessionFactoryScope scope) {
		final var metamodel = scope.getSessionFactory().getMappingMetamodel();
		final var firstGenerator = assertInstanceOf( SequenceStyleGenerator.class,
				metamodel.getEntityDescriptor( FirstConsumer.class ).getGenerator() );
		final var secondGenerator = assertInstanceOf( SequenceStyleGenerator.class,
				metamodel.getEntityDescriptor( SecondConsumer.class ).getGenerator() );
		assertNotSame( firstGenerator, secondGenerator );
		assertNotSame( firstGenerator.getOptimizer(), secondGenerator.getOptimizer() );
		assertInstanceOf( PooledLoOptimizer.class, firstGenerator.getOptimizer() );
		assertInstanceOf( PooledLoThreadLocalOptimizer.class, secondGenerator.getOptimizer() );
		assertEquals( 20, firstGenerator.getOptimizer().getIncrementSize() );
		assertEquals( 20, secondGenerator.getOptimizer().getIncrementSize() );
		assertEquals( firstGenerator.getDatabaseStructure().getPhysicalName(),
				secondGenerator.getDatabaseStructure().getPhysicalName() );

		scope.inTransaction( session -> {
			final var first = new FirstConsumer();
			final var second = new SecondConsumer();
			session.persist( first );
			session.persist( second );
			assertNotNull( first.id );
			assertNotNull( second.id );
			assertNotEquals( first.id, second.id );
		} );
	}

	//tag::identifiers-generator-optimizer-sequence[]
	@Entity(name = "LocalSequenceEntity")
	public static class LocalSequenceEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "local_sequence_generator")
		@SequenceGenerator(name = "local_sequence_generator", sequenceName = "local_sequence", allocationSize = 20)
		@GeneratorOptimizer(StandardOptimizerDescriptor.POOLED_LO)
		private Long id;
	}
	//end::identifiers-generator-optimizer-sequence[]

	//tag::identifiers-generator-optimizer-table[]
	@Entity(name = "LocalTableEntity")
	public static class LocalTableEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "local_table_generator")
		@TableGenerator(
				name = "local_table_generator",
				table = "identifier_pools",
				pkColumnName = "segment_name",
				valueColumnName = "next_value",
				pkColumnValue = "local_table_entity",
				allocationSize = 20
		)
		@GeneratorOptimizer(StandardOptimizerDescriptor.POOLED_LO)
		private Long id;
	}
	//end::identifiers-generator-optimizer-table[]

	//tag::identifiers-generator-optimizer-global[]
	@Entity(name = "GeneratorDefinitions")
	@SequenceGenerator(name = "shared_generator", sequenceName = "shared_sequence", allocationSize = 20)
	public static class GeneratorDefinitions {
		@Id
		private Long id;
	}

	@Entity(name = "FirstConsumer")
	public static class FirstConsumer {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shared_generator")
		@GeneratorOptimizer(StandardOptimizerDescriptor.POOLED_LO)
		private Long id;
	}

	@Entity(name = "SecondConsumer")
	public static class SecondConsumer {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shared_generator")
		@GeneratorOptimizer(StandardOptimizerDescriptor.POOLED_LOTL)
		private Long id;
	}
	//end::identifiers-generator-optimizer-global[]
}
