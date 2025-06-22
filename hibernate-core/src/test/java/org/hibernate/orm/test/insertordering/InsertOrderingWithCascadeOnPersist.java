/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TableGenerator;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
@DomainModel(
		annotatedClasses = {
				InsertOrderingWithCascadeOnPersist.MarketBid.class,
				InsertOrderingWithCascadeOnPersist.MarketBidGroup.class,
				InsertOrderingWithCascadeOnPersist.MarketResult.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.ORDER_INSERTS, value = "true"),
				@Setting(name = AvailableSettings.ORDER_UPDATES, value = "true")
		}
)
public class InsertOrderingWithCascadeOnPersist {

	@Test
	@JiraKey(value = "HHH-11768")
	public void testInsertOrderingAvoidingForeignKeyConstraintViolation(SessionFactoryScope scope) {
		Long bidId = scope.fromTransaction( session -> {
			// create MarketBid and Group
			final MarketBidGroup group = new MarketBidGroup();
			final MarketBid bid = new MarketBid();
			bid.setGroup( group );
			session.persist( bid );
			return bid.getId();
		} );

		// This block resulted in a Foreign Key ConstraintViolation because the inserts were ordered incorrectly.
		scope.inTransaction( session -> {
			// Add marketResult to existing Bid
			final MarketBid bid = session.getReference( MarketBid.class, bidId );
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

		private String bidNote;

		@ManyToOne(optional = false, cascade = CascadeType.PERSIST)
		private MarketBidGroup group;

		public Long getId() {
			return id;
		}

		public void setGroup(MarketBidGroup group) {
			this.group = group;
		}

		public String getBidNote() {
			return bidNote;
		}

		public void setBidNote(String bidNote) {
			this.bidNote = bidNote;
		}
	}

	@Entity(name = "MarketBidGroup")
	@Access(AccessType.FIELD)
	public static class MarketBidGroup {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "ID_TABLE_2")
		@TableGenerator(name = "ID_TABLE_2", pkColumnValue = "MarketBidGroup", allocationSize = 10000)
		private Long id;

		private String bidGroup;

		@OneToMany(mappedBy = "group")
		private final Set<MarketBid> marketBids = new HashSet<>();

		public void addMarketBid(MarketBid marketBid) {
			this.marketBids.add( marketBid );
		}

		public String getBidGroup() {
			return bidGroup;
		}

		public void setBidGroup(String bidGroup) {
			this.bidGroup = bidGroup;
		}
	}

	@Entity(name = "MarketResult")
	@Access(AccessType.FIELD)
	public static class MarketResult {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "ID_TABLE_3")
		@TableGenerator(name = "ID_TABLE_3", pkColumnValue = "MarketResult", allocationSize = 10000)
		private Long id;

		private String resultNote;

		@ManyToOne(optional = false)
		private MarketBid marketBid;

		public void setMarketBid(MarketBid marketBid) {
			this.marketBid = marketBid;
		}

		public String getResultNote() {
			return resultNote;
		}

		public void setResultNote(String resultNote) {
			this.resultNote = resultNote;
		}
	}
}
