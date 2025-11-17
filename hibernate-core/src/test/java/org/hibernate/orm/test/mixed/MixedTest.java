/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mixed;

import org.hibernate.Session;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.SkipLog;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.getLobHelper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gavin King
 */
@SkipForDialect(dialectClass = SybaseDialect.class, majorVersion = 15)
@DomainModel(xmlMappings = "org/hibernate/orm/test/mixed/Item.hbm.xml")
@SessionFactory
public class MixedTest {

	@Test
	public void testMixedInheritance(SessionFactoryScope scope) {
		Document doc = new Document();
		SecureDocument doc2 = new SecureDocument();
		try (final Session session = scope.getSessionFactory()
				.withOptions()
				.interceptor( new DocumentInterceptor() )
				.openSession()) {
			session.beginTransaction();
			try {
				Folder f = new Folder();
				f.setName( "/" );
				session.persist( f );

				doc.setName( "Hibernate in Action" );
				doc.setContent( getLobHelper().createBlob( "blah blah blah".getBytes() ) );
				doc.setParent( f );
				session.persist( doc );

				doc2.setName( "Secret" );
				doc2.setContent( getLobHelper().createBlob( "wxyz wxyz".getBytes() ) );
				// SybaseASE15Dialect only allows 7-bits in a byte to be inserted into a tinyint
				// column (0 <= val < 128)
				doc2.setPermissionBits( (byte) 127 );
				doc2.setOwner( "gavin" );
				doc2.setParent( f );
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

		if ( !scope.getSessionFactory().getJdbcServices().getDialect().supportsExpectedLobUsagePattern() ) {
			SkipLog.reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return;
		}

		try (final Session session = scope.getSessionFactory()
				.withOptions()
				.interceptor( new DocumentInterceptor() )
				.openSession()) {
			session.beginTransaction();
			try {
				Item id = session.getReference( Item.class, did );
				assertEquals( did, id.getId() );
				assertEquals( "Hibernate in Action", id.getName() );
				assertEquals( "/", id.getParent().getName() );

				Item id2 = session.getReference( Item.class, d2id );
				assertEquals( d2id, id2.getId() );
				assertEquals( "Secret", id2.getName() );
				assertEquals( "/", id2.getParent().getName() );

				id.setName( "HiA" );

				SecureDocument d2 = session.getReference( SecureDocument.class, d2id );
				d2.setOwner( "max" );

				session.flush();

				session.clear();

				Document d = session.getReference( Document.class, did );
				assertEquals( did, d.getId() );
				assertEquals( "HiA", d.getName() );
				assertNotNull( d.getContent() );
				assertEquals( "/", d.getParent().getName() );
				assertNotNull( d.getCreated() );
				assertNotNull( d.getModified() );

				d2 = session.getReference( SecureDocument.class, d2id );
				assertEquals( d2id, d2.getId() );
				assertEquals( "Secret", d2.getName() );
				assertNotNull( d2.getContent() );
				assertEquals( "max", d2.getOwner() );
				assertEquals( "/", d2.getParent().getName() );
				// SybaseASE15Dialect only allows 7-bits in a byte to be inserted into a tinyint
				// column (0 <= val < 128)
				assertEquals( (byte) 127, d2.getPermissionBits() );
				assertNotNull( d2.getCreated() );
				assertNotNull( d2.getModified() );

				session.remove( d.getParent() );
				session.remove( d );
				session.remove( d2 );
			}
			finally {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
			}
		}
	}
}
