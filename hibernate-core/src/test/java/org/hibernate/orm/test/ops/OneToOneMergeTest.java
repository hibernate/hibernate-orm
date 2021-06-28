/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ops;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author localEvg
 */
@TestForIssue(jiraKey = "HHH-12436")
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