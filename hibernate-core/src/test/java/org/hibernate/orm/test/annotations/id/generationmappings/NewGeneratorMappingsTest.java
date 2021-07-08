/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.id.generationmappings;

import org.hibernate.cfg.Environment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test mapping the {@link javax.persistence.GenerationType GenerationTypes} to the corresponding
 * hibernate generators using the new scheme
 *
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@DomainModel(
		annotatedClasses = {
				MinimalSequenceEntity.class,
				CompleteSequenceEntity.class,
				AutoEntity.class,
				MinimalTableEntity.class,
				DedicatedSequenceEntity1.class,
				DedicatedSequenceEntity2.class,
				AbstractTPCAutoEntity.class,
				TPCAutoEntity1.class
		},
		annotatedPackageNames = {
				"org.hibernate.orm.test.annotations.id.generationmappings"
		}
)
@SessionFactory(
		exportSchema = false
)
@ServiceRegistry(settings = @Setting(name =Environment.HBM2DDL_AUTO, value = ""))
public class NewGeneratorMappingsTest  {


	@Test
	public void testMinimalSequenceEntity(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getEntityPersister( MinimalSequenceEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( MinimalSequenceEntity.SEQ_NAME, seqGenerator.getDatabaseStructure().getName() );
		// 1 is the annotation default
		assertEquals( 1, seqGenerator.getDatabaseStructure().getInitialValue() );
		// 50 is the annotation default
		assertEquals( 50, seqGenerator.getDatabaseStructure().getIncrementSize() );
		assertFalse( NoopOptimizer.class.isInstance( seqGenerator.getOptimizer() ) );
	}

	@Test
	public void testCompleteSequenceEntity(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getEntityPersister( CompleteSequenceEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( 1000, seqGenerator.getDatabaseStructure().getInitialValue() );
		assertEquals( 52, seqGenerator.getDatabaseStructure().getIncrementSize() );
		assertFalse( NoopOptimizer.class.isInstance( seqGenerator.getOptimizer() ) );
	}

	@Test
	public void testAutoEntity(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getEntityPersister( AutoEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( "AutoEntity_SEQ", seqGenerator.getDatabaseStructure().getName() );
		assertEquals( SequenceStyleGenerator.DEFAULT_INITIAL_VALUE, seqGenerator.getDatabaseStructure().getInitialValue() );
		assertEquals( SequenceStyleGenerator.DEFAULT_INCREMENT_SIZE, seqGenerator.getDatabaseStructure().getIncrementSize() );
	}

	@Test
	public void testTablePerClassAutoEntity(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getEntityPersister( AbstractTPCAutoEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( "AbstractTPCAutoEntity_SEQ", seqGenerator.getDatabaseStructure().getName() );
		assertEquals( SequenceStyleGenerator.DEFAULT_INITIAL_VALUE, seqGenerator.getDatabaseStructure().getInitialValue() );
		assertEquals( SequenceStyleGenerator.DEFAULT_INCREMENT_SIZE, seqGenerator.getDatabaseStructure().getIncrementSize() );
	}

	@Test
	public void testMinimalTableEntity(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getEntityPersister( MinimalTableEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( TableGenerator.class.isInstance( generator ) );
		TableGenerator tabGenerator = (TableGenerator) generator;
		assertEquals( MinimalTableEntity.TBL_NAME, tabGenerator.getTableName() );
		assertEquals( TableGenerator.DEF_SEGMENT_COLUMN, tabGenerator.getSegmentColumnName() );
		assertEquals( "MINIMAL_TBL", tabGenerator.getSegmentValue() );
		assertEquals( TableGenerator.DEF_VALUE_COLUMN, tabGenerator.getValueColumnName() );
		// 0 is the annotation default, but its expected to be treated as 1
		assertEquals( 1, tabGenerator.getInitialValue() );
		// 50 is the annotation default
		assertEquals( 50, tabGenerator.getIncrementSize() );
		assertTrue( PooledOptimizer.class.isInstance( tabGenerator.getOptimizer() ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6790")
	public void testSequencePerEntity(SessionFactoryScope scope) {
		// Checking first entity.
		EntityPersister persister = scope.getSessionFactory().getEntityPersister( DedicatedSequenceEntity1.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals(
				"DEDICATED_SEQ_TBL1" + DedicatedSequenceEntity1.SEQUENCE_SUFFIX,
				seqGenerator.getDatabaseStructure().getName()
		);

		// Checking second entity.
		persister = scope.getSessionFactory().getEntityPersister( DedicatedSequenceEntity2.class.getName() );
		generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals(
				"DEDICATED_SEQ_TBL2" + DedicatedSequenceEntity1.SEQUENCE_SUFFIX,
				seqGenerator.getDatabaseStructure().getName()
		);
	}
}
