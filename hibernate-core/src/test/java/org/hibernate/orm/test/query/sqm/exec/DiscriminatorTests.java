/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.exec;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.DomesticVendor;
import org.hibernate.testing.orm.domain.retail.ForeignVendor;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
public class DiscriminatorTests extends BaseSessionFactoryFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		StandardDomainModel.RETAIL.getDescriptor().applyDomainModel( metadataSources );
	}

	@BeforeEach
	public void setUpTestData() {
		inTransaction(
				session -> {
					session.persist( new ForeignVendor( 1, "ForeignVendor", "Vendor, Inc." ) );
					session.persist( new DomesticVendor( 2, "DomesticVendor", "Vendor, Inc." ) );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelection() {
		inTransaction(
				session -> {
					final Query<Class> query = session.createQuery( "select type( v ) from Vendor v", Class.class );
					final List<Class> list = query.list();
					assertThat( list ).hasSize( 2 );
					assertThat( list ).containsOnly( DomesticVendor.class, ForeignVendor.class );

					assertThat( list ).contains( DomesticVendor.class );
					assertThat( list ).contains( ForeignVendor.class );
				}
		);
	}
}
