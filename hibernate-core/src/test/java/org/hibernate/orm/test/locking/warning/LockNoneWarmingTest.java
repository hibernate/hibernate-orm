/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.warning;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static jakarta.persistence.LockModeType.NONE;
import static org.junit.Assert.assertFalse;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10513")
public class LockNoneWarmingTest extends BaseCoreFunctionalTestCase {

	private Triggerable triggerable;

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, LockNoneWarmingTest.class.getName() )
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
	public void testQuerySetLockModeNONEDoNotLogAWarnMessageWhenTheDialectUseFollowOnLockingIsTrue() {
		try (Session s = openSession();) {
			final Query query = s.createQuery( "from Item i join i.bids b where name = :name", Object[].class );
			query.setParameter( "name", "ZZZZ" );
			query.setLockMode( NONE );
			query.list();
			assertFalse( "Log message was not triggered", triggerable.wasTriggered() );
		}
	}

	@Entity(name = "Item")
	@Table(name = "ITEM")
	public static class Item implements Serializable {
		@Id
		String name;

		@Column(name = "i_comment")
		String comment;

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

		@Column(name = "b_comment")
		String comment;
	}
}
