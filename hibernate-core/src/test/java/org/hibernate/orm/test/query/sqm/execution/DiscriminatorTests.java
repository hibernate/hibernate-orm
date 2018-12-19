/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.support.domains.AvailableDomainModel;
import org.hibernate.orm.test.support.domains.retail.CardPayment;
import org.hibernate.orm.test.support.domains.retail.CashPayment;
import org.hibernate.orm.test.support.domains.retail.DomesticVendor;
import org.hibernate.orm.test.support.domains.retail.ForeignVendor;
import org.hibernate.orm.test.support.domains.retail.Vendor;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.junit5.FailureExpected;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class DiscriminatorTests extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		AvailableDomainModel.RETAIL.getDomainModel().applyDomainModel( metadataSources );
	}

	@BeforeEach
	public void setUpTestData() {
		sessionFactoryScope().inTransaction(
				session -> {
					session.save( new ForeignVendor( 1, "ForeignVendor", "Vendor, Inc." ) );
					session.save( new DomesticVendor( 2, "DomesticVendor", "Vendor, Inc." ) );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData() {
		sessionFactoryScope().inTransaction(
				session -> session.createQuery( "select v from Vendor v", Vendor.class ).stream().forEach( session::delete )
		);
	}

	@Test
	@FailureExpected( "Discriminator mappings not yet implemented" )
	public void testSelection() {
		sessionFactoryScope().inTransaction(
				session -> {
					final QueryImplementor query = session.createQuery( "select type( v ) from Vendor v" );
					final List list = query.list();
					assertThat( list.size(), is( 2 ) );
					assert list.contains( DomesticVendor.class );
					assert list.contains( ForeignVendor.class );
				}
		);
	}
}
