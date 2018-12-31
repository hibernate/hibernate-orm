/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.graph.spi.RootGraphImplementor;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inSession;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;

/**
 * @author Steve Ebersole
 */
public class EntityGraphFunctionalTests extends AbstractEntityGraphTest {
	@Test
	@TestForIssue( jiraKey = "HHH-13175")
	public void testSubsequentSelectFromFind() {
		inTransaction(
				entityManagerFactory(),
				session -> {
					GraphParsingTestEntity e1 = new GraphParsingTestEntity();
					e1.setId( "1" );
					e1.setName( "First GraphParsingTestEntity" );

					GraphParsingTestEntity e2 = new GraphParsingTestEntity();
					e2.setId( "2" );
					e2.setName( "Second GraphParsingTestEntity" );

					GraphParsingTestEntity e3 = new GraphParsingTestEntity();
					e3.setId( "3" );
					e3.setName( "Third GraphParsingTestEntity" );

					e2.setLinkToOne( e1 );

					Map<GraphParsingTestEntity, GraphParsingTestEntity> map = new HashMap<>();
					map.put( e2, e1 );
					e3.setMap( map );

					session.save( e1 );
					session.save( e2 );
					session.save( e3 );
				}
		);

		try {
			final RootGraphImplementor<GraphParsingTestEntity> graph = parseGraph( "map" );
			inTransaction(
					entityManagerFactory(),
					session -> {
						final GraphParsingTestEntity entity = session.find(
								GraphParsingTestEntity.class,
								"1",
								Collections.singletonMap( GraphSemantic.FETCH.getJpaHintName(), graph )
						);
					}
			);
		}
		finally {
			inTransaction(
					entityManagerFactory(),
					session -> session.createQuery( "delete from GraphParsingTestEntity" ).executeUpdate()
			);
		}
	}
}
