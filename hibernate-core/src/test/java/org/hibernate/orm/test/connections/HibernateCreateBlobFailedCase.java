/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import static org.hibernate.cfg.AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test originally developed to verify and fix HHH-5550
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting( name=CURRENT_SESSION_CONTEXT_CLASS, value = "thread"))
@DomainModel(annotatedClasses = { Silly.class, Other.class })
@SessionFactory
public class HibernateCreateBlobFailedCase {

	@Test
	public void testLobCreation(SessionFactoryScope factoryScope) throws SQLException {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();
		Blob blob = session.getLobHelper().createBlob( new byte[] {} );
		blob.free();
		Clob clob = session.getLobHelper().createClob( "Steve" );
		clob.free();
		session.getTransaction().commit();
		assertFalse( session.isOpen() );
	}

}
