/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.TableGenerator;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class InsertOrderingWithCascadeOnPersist extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MarketBid.class, MarketBidGroup.class, MarketResult.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.ORDER_INSERTS, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.ORDER_UPDATES, Boolean.TRUE.toString() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11768")
	public void testInsertOrderingAvoidingForeignKeyConstraintViolation() {
		Long bidId = doInHibernate( this::sessionFactory, session -> {
			// create MarketBid and Group
			final MarketBidGroup group = new MarketBidGroup();
			final MarketBid bid = new MarketBid();
			bid.setGroup( group );
			session.persist( bid );
			return bid.getId();
		} );

		// This block resulted in a Foreign Key ConstraintViolation because the inserts were ordered incorrectly.
		doInHibernate( this::sessionFactory, session -> {
			// Add marketResult to existing Bid
			final MarketBid bid = session.load( MarketBid.class, bidId );
			final MarketResult result = new MarketResult();
			result.setMarketBid( bid );
			session.persist( result );
			// create new MarketBid, Group and Result
			final MarketBidGroup newGroup = new MarketBidGroup();
			final MarketBid newBid = new MarketBid();
			newBid.setGroup( newGroup );
			final MarketResult newResult = new MarketResult();
			newResult.setMarketBid( newBid );
			session.persist( newBid );
			session.persist( newResult );
		} );
	}

	@Entity(name = "MarketBid")
	@Access(AccessType.FIELD)
	public static class MarketBid {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "ID_TABLE")
		@TableGenerator(name = "ID_TABLE", pkColumnValue = "MarketBid", allocationSize = 10000)
		private Long id;

		@ManyToOne(optional = false, cascade = CascadeType.PERSIST)
		private MarketBidGroup group;

		public Long getId() {
			return id;
		}

		public void setGroup(MarketBidGroup group) {
			this.group = group;
		}
	}

	@Entity(name = "MarketBidGroup")
	@Access(AccessType.FIELD)
	public static class MarketBidGroup {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "ID_TABLE_2")
		@TableGenerator(name = "ID_TABLE_2", pkColumnValue = "MarketBidGroup", allocationSize = 10000)
		private Long id;

		@OneToMany(mappedBy = "group")
		private final Set<MarketBid> marketBids = new HashSet<>();

		public void addMarketBid(MarketBid marketBid) {
			this.marketBids.add(marketBid);
		}
	}

	@Entity(name = "MarketResult")
	@Access(AccessType.FIELD)
	public static class MarketResult {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "ID_TABLE_3")
		@TableGenerator(name = "ID_TABLE_3", pkColumnValue = "MarketResult", allocationSize = 10000)
		private Long id;

		@ManyToOne(optional = false)
		private MarketBid marketBid;

		public void setMarketBid(MarketBid marketBid) {
			this.marketBid = marketBid;
		}

	}
}
