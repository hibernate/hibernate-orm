/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;

import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class CrossJoinTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
	}

	@Test
	public void testSimpleCrossJoin() {
		sessionFactoryScope().inTransaction(
				session -> session.createQuery( "from SimpleEntity e1, SimpleEntity e2 where e1.id = e2.id and e1.someDate = {d '2018-01-01'}" ).list()
		);
	}
}
