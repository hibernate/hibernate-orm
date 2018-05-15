/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.conversions;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.converters.Account;
import org.hibernate.orm.test.support.domains.converters.Status;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class BasicConversionsTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Account.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testBasicConversions() {
		final Account initialAccount = new Account(
				1,
				Status.CREATED,
				Status.INITIALIZING,
				Status.ACTIVE
		);

		sessionFactoryScope().inTransaction( session -> session.save( initialAccount ) );

		sessionFactoryScope().inTransaction(
				session -> {
					final Account loaded = session.get( Account.class, 1 );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getLoginStatus(), equalTo( Status.CREATED ) );
					assertThat( loaded.getSystemAccessStatus(), equalTo( Status.INITIALIZING ) );
					assertThat( loaded.getServiceStatus(), equalTo( Status.ACTIVE ) );
				}
		);

		// todo (6.0) : good test for "parameter inferring"
//		sessionFactoryScope().inTransaction(
//				session -> {
//					final Account loaded = session.createQuery( "from Account a where a.loginStatus = :status", Account.class ).setParameter( "status", Status.CREATED ).uniqueResult();
//					assertThat( loaded, notNullValue() );
//					assertThat( loaded.getLoginStatus(), equalTo( Status.CREATED ) );
//					assertThat( loaded.getSystemAccessStatus(), equalTo( Status.INITIALIZING ) );
//					assertThat( loaded.getServiceStatus(), equalTo( Status.ACTIVE ) );
//				}
//		);

	}
}
