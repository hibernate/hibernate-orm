/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS;

/**
 * @author Luca Domenichini
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-13663")
@ServiceRegistry(settings = @Setting(name = CURRENT_SESSION_CONTEXT_CLASS, value = "thread"))
@SessionFactory
public class TestHibernateFlushModeOnThreadLocalInactiveTransaction {
	@Test
	public void testHibernateFlushModeOnInactiveTransaction(SessionFactoryScope factoryScope) {
		try (Session s = factoryScope.getSessionFactory().getCurrentSession()) {
			// this should not throw even within an inactive transaction
			s.setHibernateFlushMode( FlushMode.AUTO );
		}
	}

}
