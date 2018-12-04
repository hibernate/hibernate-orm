/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.conversions;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.helpdesk.Account;
import org.hibernate.orm.test.support.domains.helpdesk.Status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

	@BeforeEach
	public void createData() {
		final Account initialAccount = new Account(
				1,
				Status.CREATED,
				Status.INITIALIZING,
				Status.ACTIVE
		);

		sessionFactoryScope().inTransaction( session -> session.save( initialAccount ) );
	}

	@AfterEach
	public void cleanData() {
		sessionFactoryScope().inTransaction(
				session -> session.delete( session.byId( Account.class ).load( 1 ) )
		);
	}

	@Test
	public void testBasicConversions() {
		sessionFactoryScope().inTransaction(
				session -> {
					final Account loaded = session.get( Account.class, 1 );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getLoginStatus(), equalTo( Status.CREATED ) );
					assertThat( loaded.getSystemAccessStatus(), equalTo( Status.INITIALIZING ) );
					assertThat( loaded.getServiceStatus(), equalTo( Status.ACTIVE ) );
				}
		);
	}

	@Test
	public void testSqmTypeInference() {
		sessionFactoryScope().inTransaction(
				session -> {
					Account loaded = session.createQuery( "from Account a where a.loginStatus = :status", Account.class )
							.setParameter( "status", Status.CREATED )
							.uniqueResult();

					assertThat( loaded, notNullValue() );

					assertThat( loaded.getLoginStatus(), equalTo( Status.CREATED ) );
					assertThat( loaded.getSystemAccessStatus(), equalTo( Status.INITIALIZING ) );
					assertThat( loaded.getServiceStatus(), equalTo( Status.ACTIVE ) );


					// after this, asserting not-null is enough

					loaded = session.createQuery( "from Account a where a.loginStatus <> :status", Account.class )
							.setParameter( "status", Status.INITIALIZING )
							.uniqueResult();

					assertThat( loaded, notNullValue() );

					loaded = session.createQuery( "from Account a where a.loginStatus >= :status", Account.class )
							.setParameter( "status", Status.CREATED )
							.uniqueResult();

					assertThat( loaded, notNullValue() );

					loaded = session.createQuery( "from Account a where a.loginStatus < :status", Account.class )
							.setParameter( "status", Status.INACTIVE )
							.uniqueResult();

					assertThat( loaded, notNullValue() );

					loaded = session.createQuery( "from Account a where a.loginStatus <= :status", Account.class )
							.setParameter( "status", Status.INACTIVE )
							.uniqueResult();

					assertThat( loaded, notNullValue() );
				}
		);
	}
}
