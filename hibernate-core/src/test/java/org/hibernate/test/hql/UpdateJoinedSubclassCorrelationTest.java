package org.hibernate.test.hql;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import java.util.List;

import static javax.persistence.InheritanceType.JOINED;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@TestForIssue(jiraKey = "HHH-13169")
public class UpdateJoinedSubclassCorrelationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Master.class, SubMaster.class, Detail.class };
	}

	@Test
	public void testJoinedSubclassUpdateWithCorrelation() {
		// prepare
		doInJPA( this::entityManagerFactory, entityManager -> {
			Master m1 = new SubMaster( 1, null );
			entityManager.persist( m1 );
			Detail d11 = new Detail( 10, m1 );
			entityManager.persist( d11 );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			// DO NOT CHANGE this query: it used to trigger a very specific bug caused
			// by the root table alias being added to the generated subquery instead of the table name
			String u = "update SubMaster m set name = (select 'test' from Detail d where d.master = m)";
			Query updateQuery = entityManager.createQuery( u );
			updateQuery.executeUpdate();

			// so check if the name of the SubMaster has been correctly updated
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Master> query = builder.createQuery( Master.class );
			query.select( query.from( Master.class ) );
			List<Master> masters = entityManager.createQuery( query ).getResultList();
			Assert.assertEquals( 1, masters.size() );
			Assert.assertEquals( "test", ((SubMaster) masters.get(0)).name );
		} );
	}

	@Inheritance(strategy = JOINED)
	@Entity(name = "Master")
	public static abstract class Master {
		@Id
		private Integer id;

		public Master() { }

		public Master( Integer id ) {
			this.id = id;
		}
	}

	@Entity(name = "SubMaster")
	public static class SubMaster extends Master {
		private String name;

		public SubMaster() { }

		public SubMaster( Integer id, String name ) {
			super(id);
			this.name = name;
		}
	}

	@Entity(name = "Detail")
	public static class Detail {
		@Id
		private Integer id;

		@ManyToOne(optional = false)
		private Master master;

		public Detail( Integer id, Master master ) {
			this.id = id;
			this.master = master;
		}
	}
}

