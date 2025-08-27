/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interfaceproxy;

import org.hibernate.Session;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.getLobHelper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = { "org/hibernate/orm/test/interfaceproxy/Item.hbm.xml" }
)
@SessionFactory
public class InterfaceProxyTest {

	@Test
	@RequiresDialectFeature(
			feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class,
			comment = "database/driver does not support expected LOB usage pattern"
	)

	@SkipForDialect(dialectClass = AltibaseDialect.class, majorVersion = 7, minorVersion = 1,
					reason = "Altibase 7.1 lob column cannot be not null")
	public void testInterfaceProxies(SessionFactoryScope scope) {
		Document doc = new DocumentImpl();
		SecureDocument doc2 = new SecureDocumentImpl();
		try (Session session = openSession( scope )) {
			session.beginTransaction();
			try {
				doc.setName( "Hibernate in Action" );
				doc.setContent( getLobHelper().createBlob( "blah blah blah".getBytes() ) );
				session.persist( doc );
				doc2.setName( "Secret" );
				doc2.setContent( getLobHelper().createBlob( "wxyz wxyz".getBytes() ) );
				// SybaseASE15Dialect only allows 7-bits in a byte to be inserted into a tinyint
				// column (0 <= val < 128)
				doc2.setPermissionBits( (byte) 127 );
				doc2.setOwner( "gavin" );
				session.persist( doc2 );
				session.getTransaction().commit();
			}
			finally {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
			}
		}
		Long did = doc.getId();
		Long d2id = doc2.getId();

		try (Session session = openSession( scope )) {
			session.beginTransaction();
			try {
				Document d = (Document) session.getReference( ItemImpl.class, did );
				assertEquals( did, d.getId() );
				assertEquals( "Hibernate in Action", d.getName() );
				assertNotNull( d.getContent() );

				SecureDocument d2 = (SecureDocument) session.getReference( ItemImpl.class, d2id );
				assertEquals( d2id, d2.getId() );
				assertEquals( "Secret", d2.getName() );
				assertNotNull( d2.getContent() );

				session.clear();

				d = session.getReference( DocumentImpl.class, did );
				assertEquals( did, d.getId() );
				assertEquals( "Hibernate in Action", d.getName() );
				assertNotNull( d.getContent() );

				d2 = session.getReference( SecureDocumentImpl.class, d2id );
				assertEquals( d2id, d2.getId() );
				assertEquals( "Secret", d2.getName() );
				assertNotNull( d2.getContent() );
				assertEquals( "gavin", d2.getOwner() );

				//s.clear();

				d2 = session.getReference( SecureDocumentImpl.class, did );
				assertEquals( did, d2.getId() );
				assertEquals( "Hibernate in Action", d2.getName() );
				assertNotNull( d2.getContent() );

				try {
					d2.getOwner(); //CCE
					fail( "ClassCastException expected" );
				}
				catch (ClassCastException cce) {
					//correct
				}

				session.createQuery( "delete ItemImpl" ).executeUpdate();
			}
			finally {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
			}
		}
	}

	private Session openSession(SessionFactoryScope scope) {
		return scope.getSessionFactory()
				.withOptions()
				.interceptor( new DocumentInterceptor() )
				.openSession();
	}
}
