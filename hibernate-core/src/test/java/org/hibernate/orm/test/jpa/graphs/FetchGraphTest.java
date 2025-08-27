/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Yaroslav Prokipchyn
 * @author Nathan Xu
 */
@JiraKey( value = "HHH-14212" )
@Jpa(annotatedClasses = {
		FetchGraphTest.LedgerRecord.class,
		FetchGraphTest.LedgerRecordItem.class,
		FetchGraphTest.BudgetRecord.class,
		FetchGraphTest.Trigger.class,
		FetchGraphTest.FinanceEntity.class
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class FetchGraphTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Trigger trigger = new Trigger();
					entityManager.persist( trigger );

					BudgetRecord budgetRecord = new BudgetRecord();
					budgetRecord.amount = 100;
					budgetRecord.trigger = trigger;
					entityManager.persist( budgetRecord );

					FinanceEntity client = new FinanceEntity();
					client.name = "client";
					FinanceEntity vendor = new FinanceEntity();
					vendor.name = "vendor";
					entityManager.persist( client );
					entityManager.persist( vendor );

					LedgerRecordItem item1 = new LedgerRecordItem();
					item1.financeEntity = client;
					LedgerRecordItem item2 = new LedgerRecordItem();
					item2.financeEntity = vendor;
					entityManager.persist( item1 );
					entityManager.persist( item2 );

					LedgerRecord ledgerRecord = new LedgerRecord();
					ledgerRecord.budgetRecord = budgetRecord;
					ledgerRecord.trigger = trigger;
					ledgerRecord.ledgerRecordItems= Arrays.asList( item1, item2 );

					item1.ledgerRecord = ledgerRecord;
					item2.ledgerRecord = ledgerRecord;

					entityManager.persist( ledgerRecord );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCollectionEntityGraph(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final EntityGraph<LedgerRecord> entityGraph = GraphParser.parse( LedgerRecord.class, "budgetRecord, ledgerRecordItems.value(financeEntity)", entityManager );
					final List<LedgerRecord> records = entityManager.createQuery( "from LedgerRecord", LedgerRecord.class )
							.setHint( GraphSemantic.FETCH.getJpaHintName(), entityGraph )
							.getResultList();
					assertThat( records.size(), is( 1 ) );
					records.forEach( record -> {
						assertFalse( Hibernate.isInitialized( record.trigger ) );
						assertTrue( Hibernate.isInitialized( record.budgetRecord ) );
						assertFalse( Hibernate.isInitialized( record.budgetRecord.trigger ) );
						assertTrue( Hibernate.isInitialized( record.ledgerRecordItems) );
						assertThat( record.ledgerRecordItems.size(), is( 2 ) );
						record.ledgerRecordItems.forEach( item -> {
							assertSame( record, item.ledgerRecord );
							assertTrue( Hibernate.isInitialized( item.financeEntity ) );
						} );
					} );
				}
		);
	}

	@Entity(name = "LedgerRecord")
	@Table(name = "LedgerRecord")
	static class LedgerRecord {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Integer id;

		@ManyToOne
		BudgetRecord budgetRecord;

		@OneToMany(mappedBy = "ledgerRecord", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@Fetch(FetchMode.SUBSELECT)
		List<LedgerRecordItem> ledgerRecordItems;

		@ManyToOne
		Trigger trigger;
	}

	@Entity(name = "LedgerRecordItem")
	@Table(name = "LedgerRecordItem")
	static class LedgerRecordItem {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Integer id;

		@ManyToOne
		LedgerRecord ledgerRecord;

		@ManyToOne
		FinanceEntity financeEntity;
	}

	@Entity(name = "BudgetRecord")
	@Table(name = "BudgetRecord")
	static class BudgetRecord {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Integer id;

		int amount;

		@ManyToOne
		Trigger trigger;
	}

	@Entity(name = "Trigger")
	@Table(name = "TriggerEntity")
	static class Trigger {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Integer id;

		String name;
	}

	@Entity(name = "FinanceEntity")
	@Table(name = "FinanceEntity")
	static class FinanceEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Integer id;

		String name;
	}
}
