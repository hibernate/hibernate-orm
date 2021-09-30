/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.entitygraph.named.basic;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/entitygraph/named/basic/orm.xml"
)
@SessionFactory
public class BasicOrmNamedEntityGraphTest {

	@Test
	void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph graph = em.getEntityGraph( "Person" );
					assertThat( graph, notNullValue() );
				}
		);
	}
}
