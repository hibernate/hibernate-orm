/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class ParameterTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		StandardDomainModel.RETAIL.getDescriptor().applyDomainModel( metadataSources );
	}

	@Test
	public void testReusedNamedParam() {
		inTransaction(
				session -> {
					session.createQuery( "from SalesAssociate p where p.name.familiarName = :name or p.name.familyName = :name" )
							.setParameter( "name", "a name" )
							.list();
				}
		);
	}

	@Test
	public void testReusedOrdinalParam() {
		inTransaction(
				session -> {
					session.createQuery( "from SalesAssociate p where p.name.familiarName = ?1 or p.name.familyName = ?1" )
							.setParameter( 1, "a name" )
							.list();
				}
		);
	}
}
