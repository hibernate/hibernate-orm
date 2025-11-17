/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author localEvg
 */
@JiraKey(value = "HHH-12436")
@Jpa(
		annotatedClasses = {
				OneToOneMergeTest.Prima.class,
				OneToOneMergeTest.Secunda.class
		}
)
public class OneToOneMergeTest {

	@Test
	public void testMerge(EntityManagerFactoryScope scope) throws Exception {

		Long primaId = scope.fromTransaction(
				entityManager -> {
					Prima prima = new Prima();
					prima.setOptionalData( null );

					entityManager.persist( prima );

					return prima.getId();
				} );

		assertNotNull( primaId );

		scope.inTransaction(
				entityManager -> {
					Prima prima = entityManager.find( Prima.class, primaId );

					Secunda sec = new Secunda();
					sec.setParent( prima );
					prima.setOptionalData( sec );

					Prima mergedPrima = entityManager.merge( prima );

					assertNotNull( mergedPrima );
				} );

	}

	@Entity(name = "Prima")
	public static class Prima implements Serializable {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		//@PrimaryKeyJoinColumn
		@OneToOne(mappedBy = "parent", optional = true, cascade = CascadeType.ALL)
		private Secunda optionalData;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Secunda getOptionalData() {
			return optionalData;
		}

		public void setOptionalData(Secunda optionalData) {
			this.optionalData = optionalData;
		}

	}

	@Entity(name = "Secunda")
	public static class Secunda implements Serializable {

		@Id
		@Column(name = "id", nullable = false)
		private Long id;

		@MapsId
		@OneToOne(optional = false)
		@JoinColumn(name = "id", nullable = false)
		private Prima parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Prima getParent() {
			return parent;
		}

		public void setParent(Prima parent) {
			this.parent = parent;
		}

	}
}
