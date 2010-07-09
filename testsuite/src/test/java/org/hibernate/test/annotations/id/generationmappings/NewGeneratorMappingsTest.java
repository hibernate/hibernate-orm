/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.id.generationmappings;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.test.annotations.TestCase;

/**
 * Test mapping the {@link javax.persistence.GenerationType GenerationTypes} to the corresponding
 * hibernate generators using the new scheme
 *
 * @author Steve Ebersole
 */
public class NewGeneratorMappingsTest extends TestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MinimalSequenceEntity.class,
				CompleteSequenceEntity.class,
				AutoEntity.class,
				MinimalTableEntity.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AnnotationConfiguration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "" );
	}

	@Override
	protected boolean recreateSchema() {
		return false;
	}

	@Override
	protected void runSchemaGeneration() {
	}

	@Override
	protected void runSchemaDrop() {
	}

	public void testMinimalSequenceEntity() {
		final EntityPersister persister = sfi().getEntityPersister( MinimalSequenceEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( MinimalSequenceEntity.SEQ_NAME, seqGenerator.getDatabaseStructure().getName() );
		// 1 is the annotation default
		assertEquals( 1, seqGenerator.getDatabaseStructure().getInitialValue() );
		// 50 is the annotation default
		assertEquals( 50, seqGenerator.getDatabaseStructure().getIncrementSize() );
		assertFalse( OptimizerFactory.NoopOptimizer.class.isInstance( seqGenerator.getOptimizer() ) );
	}

	public void testCompleteSequenceEntity() {
		final EntityPersister persister = sfi().getEntityPersister( CompleteSequenceEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( "my_catalog.my_schema."+CompleteSequenceEntity.SEQ_NAME, seqGenerator.getDatabaseStructure().getName() );
		assertEquals( 1000, seqGenerator.getDatabaseStructure().getInitialValue() );
		assertEquals( 52, seqGenerator.getDatabaseStructure().getIncrementSize() );
		assertFalse( OptimizerFactory.NoopOptimizer.class.isInstance( seqGenerator.getOptimizer() ) );
	}

	public void testAutoEntity() {
		final EntityPersister persister = sfi().getEntityPersister( AutoEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		assertEquals( SequenceStyleGenerator.DEF_SEQUENCE_NAME, seqGenerator.getDatabaseStructure().getName() );
		assertEquals( SequenceStyleGenerator.DEFAULT_INITIAL_VALUE, seqGenerator.getDatabaseStructure().getInitialValue() );
		assertEquals( SequenceStyleGenerator.DEFAULT_INCREMENT_SIZE, seqGenerator.getDatabaseStructure().getIncrementSize() );
	}

	public void testMinimalTableEntity() {
		final EntityPersister persister = sfi().getEntityPersister( MinimalTableEntity.class.getName() );
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
		assertTrue( OptimizerFactory.PooledOptimizer.class.isInstance( tabGenerator.getOptimizer() ) );
	}
}
