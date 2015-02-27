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

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.byteman.BytemanHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Sanne Grinovero
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-8788")
@RunWith(BMUnitRunner.class)
public class CriteriaLockingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Bid.class, Item.class};
	}

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.dialect.Dialect",
					targetMethod = "useFollowOnLocking",
					action = "return true",
					name = "H2DialectUseFollowOnLocking"),
			@BMRule(targetClass = "org.hibernate.internal.CoreMessageLogger_$logger",
					targetMethod = "usingFollowOnLocking",
					helper = "org.hibernate.testing.byteman.BytemanHelper",
					action = "countInvocation()",
					name = "countWarnings")
	})
	public void testSetLockModeNONEDoNotLogAWarnMessageWhenTheDialectUseFollowOnLockingIsTrue() {
		buildSessionFactory();
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

		assertEquals( "HHH000444 should not be called", 0, BytemanHelper.getAndResetInvocationCount() );
	}

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.dialect.Dialect",
					targetMethod = "useFollowOnLocking",
					action = "return true",
					name = "H2DialectUseFollowOnLocking"),
			@BMRule(targetClass = "org.hibernate.internal.CoreMessageLogger_$logger",
					targetMethod = "usingFollowOnLocking",
					helper = "org.hibernate.testing.byteman.BytemanHelper",
					action = "countInvocation()",
					name = "countWarnings")
	})
	public void testSetLockModeDifferentFromNONELogAWarnMessageWhenTheDialectUseFollowOnLockingIsTrue() {
		buildSessionFactory();
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

		assertEquals( "HHH000444 should not be called", 1, BytemanHelper.getAndResetInvocationCount() );
	}
}
