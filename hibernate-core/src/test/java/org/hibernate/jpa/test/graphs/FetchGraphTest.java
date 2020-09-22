package org.hibernate.jpa.test.graphs;

import java.util.Arrays;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Yaroslav Prokipchyn
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14212" )
public class FetchGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				LedgerRecord.class,
				LedgerRecordItem.class,
				BudgetRecord.class,
				Trigger.class,
				FinanceEntity.class
		};
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
		} );
	}

	@Test
	public void testCollectionEntityGraph() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
		} );
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

