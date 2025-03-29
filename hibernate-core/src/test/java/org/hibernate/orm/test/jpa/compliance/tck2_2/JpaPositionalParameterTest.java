/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import org.hibernate.query.Query;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Parameter;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class JpaPositionalParameterTest extends AbstractJPATest {

	@Test
	public void testPositionalParameters() {
		inTransaction(
				session -> {
					Query query = session.createQuery( "select i from Item i where name = ?1 or name = ?2" );
					for ( Parameter<?> parameter : query.getParameters() ) {
						assertThat( parameter.getPosition(), notNullValue() );
						assertThat( parameter.getPosition(), either( is( 1 ) ).or( is( 2 ) ) );
					}
				}
		);
	}
}
