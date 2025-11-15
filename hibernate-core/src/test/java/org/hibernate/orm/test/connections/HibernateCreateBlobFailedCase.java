/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Hibernate.getLobHelper;

/**
 * Test originally developed to verify and fix HHH-5550
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/connections/Silly.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.CURRENT_SESSION_CONTEXT_CLASS, value = "thread")
		}
)
public class HibernateCreateBlobFailedCase {

	@Test
	public void testLobCreation(SessionFactoryScope scope) throws SQLException {
		try (Session session = scope.getSessionFactory().getCurrentSession()) {
			session.beginTransaction();
			try {
				Blob blob = getLobHelper().createBlob( new byte[] {} );
				blob.free();
				Clob clob = getLobHelper().createClob( "Steve" );
				clob.free();
				session.getTransaction().commit();
			}
			finally {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
			}
			assertThat( session.isOpen() ).isFalse();
		}
	}

}
