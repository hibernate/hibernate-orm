/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs.named.basic;

import javax.persistence.EntityGraph;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNamedEntityGraphTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testIt() {
		EntityGraph graph = getOrCreateEntityManager().getEntityGraph( "Person" );
		assertNotNull( graph );
	}
}
