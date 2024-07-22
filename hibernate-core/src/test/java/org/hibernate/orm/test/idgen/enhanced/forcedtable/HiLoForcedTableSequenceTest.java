/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.enhanced.forcedtable;

import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/idgen/enhanced/forcedtable/HiLo.hbm.xml"
)
@SessionFactory
public class HiLoForcedTableSequenceTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
        final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getIdentifierGenerator(), instanceOf( SequenceStyleGenerator.class ) );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getIdentifierGenerator();
		assertThat( generator.getDatabaseStructure(), instanceOf( TableStructure.class ) );
		assertThat( generator.getOptimizer(), instanceOf( HiLoOptimizer.class ) );

		final HiLoOptimizer optimizer = (HiLoOptimizer) generator.getOptimizer();
		int increment = optimizer.getIncrementSize();

		scope.inTransaction(
				(s) -> {
					for ( int i = 0; i < increment; i++ ) {
						final Entity entity = new Entity( "" + ( i + 1 ) );
						s.persist( entity );

						long expectedId = i + 1;
						assertThat( entity.getId().longValue(), is( expectedId ) );
						assertThat( ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue(), is( 1L ) );
						assertThat( ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue(), is( i + 1L ) );
						assertThat( ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue(), is( increment + 1L ) );
					}

					// now force a "clock over"
					final Entity entity = new Entity( "" + increment );
					s.persist( entity );

					long expectedId = optimizer.getIncrementSize() + 1;
					assertThat( entity.getId().longValue(), is( expectedId ) );
					// initialization + clock-over
					assertThat( ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue(), is( 2L ) );
					assertThat( ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue(), is( increment + 1L ) );
					assertThat( ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue(), is( ( increment * 2L ) + 1L ) );
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
