package org.hibernate.test.annotations.onetomany;

import org.hibernate.annotations.JoinFormula;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue( jiraKey = "HHH-12997" )
public class OneToManyJoinFormulaTest
		extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Invoice.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE );
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Invoice rootInvoice = new Invoice();
			rootInvoice.setId(1L);
			rootInvoice.setHierarchyPath("1/");
			entityManager.persist(rootInvoice);

			Invoice childInvoice1 = new Invoice();
			childInvoice1.setId(2L);
			childInvoice1.setHierarchyPath("1/2/");
			entityManager.persist(childInvoice1);

			Invoice childInvoice2 = new Invoice();
			childInvoice2.setId(3L);
			childInvoice2.setHierarchyPath("1/3/");
			entityManager.persist(childInvoice2);
		});
	}

	@Test
	public void testBulkUpdate() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Invoice rootInvoice = entityManager.createQuery(
					"select inv from Invoice inv " +
					"left join fetch inv.childInvoices " +
					"where inv.id = 1", Invoice.class).getSingleResult();

			assertEquals(2, rootInvoice.getChildInvoices().size());
		});
	}

	@Entity(name = "Invoice")
	public static class Invoice {

		@Id
		private Long id;

		@Column(name = "hierarchy_path")
		private String hierarchyPath;

		@OneToMany(fetch = FetchType.LAZY)
		@JoinFormula(value = "hierarchy_path like id || '/_%'", referencedColumnName = "hierarchy_path")
		private Set<Invoice> childInvoices = new HashSet<>();

		public String getHierarchyPath() {
			return hierarchyPath;
		}

		public void setHierarchyPath(String hierarchyPath) {
			this.hierarchyPath = hierarchyPath;
		}

		public Set<Invoice> getChildInvoices() {
			return childInvoices;
		}

		public void setChildInvoices(Set<Invoice> childInvoices) {
			this.childInvoices = childInvoices;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}