/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences;


import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.annotations.id.sequences.entities.HibernateSequenceEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak
 */
@JiraKey(value = "HHH-6068")
@RequiresDialect(value = H2Dialect.class)
@DomainModel(
		annotatedClasses = HibernateSequenceEntity.class,
		xmlMappings = "org/hibernate/orm/test/annotations/id/sequences/orm.xml"
)
@SessionFactory(createSecondarySchemas = true)
public class HibernateSequenceTest {
	private static final String SCHEMA_NAME = "OTHER_SCHEMA";

	@Test
	public void testHibernateSequenceSchema(SessionFactoryScope scope) {
		EntityPersister persister = scope.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel().getEntityDescriptor(HibernateSequenceEntity.class.getName());
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( SequenceStyleGenerator.class.isInstance( generator ) );
		SequenceStyleGenerator seqGenerator = (SequenceStyleGenerator) generator;
		SqlStringGenerationContext sqlStringGenerationContext = scope.getSessionFactory().getSqlStringGenerationContext();
		assertEquals(
				Table.qualify( null, SCHEMA_NAME, "HibernateSequenceEntity_SEQ" ),
				sqlStringGenerationContext.format( seqGenerator.getDatabaseStructure().getPhysicalName() )
		);
	}

	@Test
	public void testHibernateSequenceNextVal(SessionFactoryScope scope) {
		HibernateSequenceEntity entity = new HibernateSequenceEntity();
		scope.inTransaction(
				session -> {
					entity.setText( "sample text" );
					session.persist( entity );
				}
		);

		assertNotNull( entity.getId() );
	}
}
