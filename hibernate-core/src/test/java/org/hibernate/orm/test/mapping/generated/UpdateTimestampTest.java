/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class UpdateTimestampTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Bid.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::mapping-generated-UpdateTimestamp-persist-example[]
			Bid bid = new Bid();
			bid.setUpdatedBy("John Doe");
			bid.setCents(150 * 100L);
			entityManager.persist(bid);
			//end::mapping-generated-UpdateTimestamp-persist-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::mapping-generated-UpdateTimestamp-update-example[]
			Bid bid = entityManager.find(Bid.class, 1L);

			bid.setUpdatedBy("John Doe Jr.");
			bid.setCents(160 * 100L);
			entityManager.persist(bid);
			//end::mapping-generated-UpdateTimestamp-update-example[]
		});
	}

	//tag::mapping-generated-provided-update-ex1[]
	@Entity(name = "Bid")
	public static class Bid {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "updated_on")
		@UpdateTimestamp
		private Date updatedOn;

		@Column(name = "updated_by")
		private String updatedBy;

		private Long cents;

		//Getters and setters are omitted for brevity

	//end::mapping-generated-provided-update-ex1[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getUpdatedOn() {
			return updatedOn;
		}

		public String getUpdatedBy() {
			return updatedBy;
		}

		public void setUpdatedBy(String updatedBy) {
			this.updatedBy = updatedBy;
		}

		public Long getCents() {
			return cents;
		}

		public void setCents(Long cents) {
			this.cents = cents;
		}
	//tag::mapping-generated-provided-update-ex1[]
	}
	//end::mapping-generated-provided-update-ex1[]
}
