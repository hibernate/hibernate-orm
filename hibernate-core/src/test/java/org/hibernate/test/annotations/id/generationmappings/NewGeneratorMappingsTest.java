/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.id.generationmappings;

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test mapping the {@link javax.persistence.GenerationType GenerationTypes} to the corresponding
 * hibernate generators using the new scheme
 *
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NewGeneratorMappingsTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MinimalSequenceEntity.class,
				CompleteSequenceEntity.class,
				AutoEntity.class,
				MinimalTableEntity.class,
				DedicatedSequenceEntity1.class,
				DedicatedSequenceEntity2.class
		};
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] { this.getClass().getPackage().getName() };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "" );
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Test
	public void testMinimalSequenceEntity() {
		final EntityPersister persister = sessionFactory().getEntityPersister( MinimalSequenceEntity.class.getName() );
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
	public void testCompleteSequenceEntity() {
		final EntityPersister persister = sessionFactory().getEntityPersister( CompleteSequenceEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( 1000, seqGenerator.getDatabaseStructure().getInitialValue() );
		assertEquals( 52, seqGenerator.getDatabaseStructure().getIncrementSize() );
		assertFalse( NoopOptimizer.class.isInstance( seqGenerator.getOptimizer() ) );
	}

	@Test
	public void testAutoEntity() {
		final EntityPersister persister = sessionFactory().getEntityPersister( AutoEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, seqGenerator.getDatabaseStructure().getName() );
		assertEquals( SequenceStyleGenerator.DEFAULT_INITIAL_VALUE, seqGenerator.getDatabaseStructure().getInitialValue() );
		assertEquals( SequenceStyleGenerator.DEFAULT_INCREMENT_SIZE, seqGenerator.getDatabaseStructure().getIncrementSize() );
	}

	@Test
	public void testMinimalTableEntity() {
		final EntityPersister persister = sessionFactory().getEntityPersister( MinimalTableEntity.class.getName() );
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
	public void testSequencePerEntity() {
		// Checking first entity.
		EntityPersister persister = sessionFactory().getEntityPersister( DedicatedSequenceEntity1.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals(
				StringHelper.unqualifyEntityName( DedicatedSequenceEntity1.class.getName() ) + DedicatedSequenceEntity1.SEQUENCE_SUFFIX,
				seqGenerator.getDatabaseStructure().getName()
		);

		// Checking second entity.
		persister = sessionFactory().getEntityPersister( DedicatedSequenceEntity2.class.getName() );
		generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals(
				DedicatedSequenceEntity2.ENTITY_NAME + DedicatedSequenceEntity1.SEQUENCE_SUFFIX,
				seqGenerator.getDatabaseStructure().getName()
		);
	}
}
