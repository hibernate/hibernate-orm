/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.naturalid;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(dialectClass = OracleDialect.class,
		reason = "Oracle do not support identity key generation")
@SkipForDialect(dialectClass = HANADialect.class,
		reason = "Hana do not support identity key generation")
@SkipForDialect(dialectClass = AltibaseDialect.class,
		reason = "Altibase do not support identity key generation")
public class MutableNaturalIdTest extends AbstractJPATest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Group.class, ClassWithIdentityColumn.class };
	}

	@Test
	public void testSimpleNaturalIdLoadAccessCacheWithUpdate() {
		inTransaction(
				session -> {
					Group g = new Group( 1, "admin" );
					session.persist( g );
				}
		);

		inTransaction(
				session -> {
					Group g = session.bySimpleNaturalId( Group.class ).load( "admin" );
					assertNotNull( g );
					Group g2 = (Group) session.bySimpleNaturalId( Group.class ).getReference( "admin" );
					assertTrue( g == g2 );
					g.setName( "admins" );
					session.flush();
					g2 = session.bySimpleNaturalId( Group.class ).getReference( "admins" );
					assertTrue( g == g2 );
				}
		);

		inTransaction(
				session ->
						session.createQuery( "delete Group" ).executeUpdate()
		);
	}

	@Test
	@JiraKey(value = "HHH-7304")
	public void testInLineSynchWithIdentityColumn() {
		inTransaction(
				session -> {
					ClassWithIdentityColumn e = new ClassWithIdentityColumn();
					e.setName( "Dampf" );
					session.persist( e );
					e.setName( "Klein" );
					assertNotNull( session.bySimpleNaturalId( ClassWithIdentityColumn.class ).load( "Klein" ) );
				}
		);
	}

}
