/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.enhanced.sequence;

import org.hibernate.Session;
import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/idgen/enhanced/sequence/Basic.hbm.xml",
				"org/hibernate/orm/test/idgen/enhanced/sequence/Dedicated.hbm.xml"
		}
)
@SessionFactory
public class BasicSequenceTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getEntityPersister( Entity.class.getName() );
		assertThat( persister.getIdentifierGenerator(), instanceOf( SequenceStyleGenerator.class ) );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getIdentifierGenerator();

		final int count = 5;

		scope.inTransaction(
				(s) -> {
					for ( int i = 0; i < count; i++ ) {
						final Entity entity = new Entity( "" + ( i + 1 ) );
						s.save( entity );

						long expectedId = i + 1;

						assertEquals( expectedId, entity.getId().longValue() );
						assertEquals( expectedId, generator.getDatabaseStructure().getTimesAccessed() );
						assertEquals( expectedId, ( (BasicHolder) generator.getOptimizer()
								.getLastSourceValue() ).getActualLongValue() );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6790")
	public void testSequencePerEntity(SessionFactoryScope scope) {
		final String overriddenEntityName = "SpecialEntity";

		final EntityPersister persister = scope.getSessionFactory().getEntityPersister( overriddenEntityName );
		assertThat( persister.getIdentifierGenerator(), instanceOf( SequenceStyleGenerator.class ) );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getIdentifierGenerator();
		assertEquals( "ID_SEQ_BSC_ENTITY" + SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX, generator.getDatabaseStructure().getName() );

		scope.inTransaction(
				(s) -> {
					Entity entity1 = new Entity( "1" );
					s.save( overriddenEntityName, entity1 );
					Entity entity2 = new Entity( "2" );
					s.save( overriddenEntityName, entity2 );

					assertEquals( 1, entity1.getId().intValue() );
					assertEquals( 2, entity2.getId().intValue() );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete Entity" ).executeUpdate()
		);
	}
}
