/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.criteria;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.logging.Logger;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.Loader;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sanne Grinovero
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-8788")
@RunWith(BMUnitRunner.class)
public class CriteriaLockingTest extends BaseCoreFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger(CoreMessageLogger.class, Loader.class.getName())
		);

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Bid.class, Item.class};
	}

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.dialect.Dialect",
					targetMethod = "useFollowOnLocking",
					action = "return true",
					name = "H2DialectUseFollowOnLocking")
	})
	public void testSetLockModeNONEDoNotLogAWarnMessageWhenTheDialectUseFollowOnLockingIsTrue() {
		buildSessionFactory();
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000444" );

		final Session s = openSession();
		final Transaction tx = s.beginTransaction();

		Item item = new Item();
		item.name = "ZZZZ";
		s.persist( item );

		s.flush();

		Criteria criteria = s.createCriteria( Item.class )
				.setLockMode( LockMode.NONE );

		criteria.list();

		tx.rollback();
		s.close();

		releaseSessionFactory();

		assertFalse( triggerable.wasTriggered() );
	}

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.dialect.Dialect",
					targetMethod = "useFollowOnLocking",
					action = "return true",
					name = "H2DialectUseFollowOnLocking")
	})
	public void testSetLockModeDifferentFromNONELogAWarnMessageWhenTheDialectUseFollowOnLockingIsTrue() {
		buildSessionFactory();
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000444" );

		final Session s = openSession();
		final Transaction tx = s.beginTransaction();

		Item item = new Item();
		item.name = "ZZZZ";
		s.persist( item );

		s.flush();

		Criteria criteria = s.createCriteria( Item.class )
				.setLockMode( LockMode.OPTIMISTIC );

		criteria.list();

		tx.rollback();
		s.close();
		releaseSessionFactory();

		assertTrue( triggerable.wasTriggered() );
	}
}
