/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.locking.warning;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;

import org.jboss.logging.Logger;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.Loader;
import org.hibernate.query.Query;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.junit.Assert.assertFalse;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10513")
@RunWith(BMUnitRunner.class)
public class LockNoneWarmingTest extends BaseCoreFunctionalTestCase {

	private Triggerable triggerable;
	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, Loader.class.getName() )
	);

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Item.class, Bid.class};
	}

	@Before
	public void setUp() {
		buildSessionFactory();
		final Set messagesPrefixes = new HashSet<>();
		messagesPrefixes.add( "HHH000444" );
		messagesPrefixes.add( "HHH000445" );
		triggerable = logInspection.watchForLogMessages( messagesPrefixes );
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Item item = new Item();
			item.name = "ZZZZ";
			session.persist( item );
		} );
	}

	@After
	public void tearDown(){
		releaseSessionFactory();
		triggerable.reset();
	}

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.dialect.Dialect",
					targetMethod = "useFollowOnLocking",
					action = "return true",
					name = "H2DialectUseFollowOnLocking")
	})
	public void testQuerySetLockModeNONEDoNotLogAWarnMessageWhenTheDialectUseFollowOnLockingIsTrue() {
		try (Session s = openSession();) {
			final Query query = s.createQuery( "from Item i join i.bids b where name = :name" );
			query.setParameter( "name", "ZZZZ" );
			query.setLockMode( "i", LockMode.NONE );
			query.setLockMode( "b", LockMode.NONE );
			query.list();
			assertFalse( "Log message was not triggered", triggerable.wasTriggered() );
		}
	}

	@Entity(name = "Item")
	@Table(name = "ITEM")
	public static class Item implements Serializable {
		@Id
		String name;

		@OneToMany(mappedBy = "item", fetch = FetchType.EAGER)
		Set<Bid> bids = new HashSet<Bid>();
	}

	@Entity(name = "Bid")
	@Table(name = "BID")
	public static class Bid implements Serializable {
		@Id
		float amount;

		@Id
		@ManyToOne
		Item item;
	}
}
