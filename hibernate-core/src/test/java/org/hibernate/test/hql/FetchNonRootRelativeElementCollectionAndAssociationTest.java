package org.hibernate.test.hql;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.*;

import java.util.HashMap;
import java.util.Map;

import static javax.persistence.CascadeType.ALL;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Moritz Becker
 */
@TestForIssue(jiraKey = "HHH-13201")
public class FetchNonRootRelativeElementCollectionAndAssociationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ProductNaturalId.class, Product.class, ProductDetail.class };
	}

	@Test
	public void testJoinedSubclassUpdateWithCorrelation() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			// DO NOT CHANGE this query: it used to trigger an error caused
			// by the origin FromElement for the association fetch being resolved to the wrong FromElement due to the
			// presence of an element collection join.
			String u = "select prod from ProductNaturalId nat inner join nat.product prod " +
					"left join fetch prod.productDetail " +
					"left join fetch prod.normalizedPricesByUnit";
			Query query = entityManager.createQuery( u, Product.class );
			query.getResultList();
		} );
	}

	@Entity(name = "ProductNaturalId")
	public class ProductNaturalId {
		@Id
		private String naturalId;
		@OneToOne(optional = false)
		private Product product;
	}

	@Entity(name = "Product")
	public class Product {
		@Id
		private Long id;
		@OneToOne(mappedBy = "product", cascade = ALL, fetch = FetchType.LAZY)
		private ProductDetail productDetail;
		@OneToOne(mappedBy = "product", cascade = ALL, fetch = FetchType.LAZY)
		private ProductNaturalId naturalId;
		@ElementCollection(fetch = FetchType.LAZY)
		private Map<String, String> normalizedPricesByUnit = new HashMap<>();
	}

	@Entity(name = "ProductDetail")
	public class ProductDetail {
		@Id
		private Long id;
		@OneToOne(optional = false)
		@JoinColumn(name = "id")
		@MapsId
		private Product product;
	}
}
