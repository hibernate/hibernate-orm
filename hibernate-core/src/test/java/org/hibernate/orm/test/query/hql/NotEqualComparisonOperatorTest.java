/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Jan Schatteman
 *  @author Mark Russell
 */
@DomainModel(
		annotatedClasses = { NotEqualComparisonOperatorTest.IntegerTextMapEntity.class }
)
@SessionFactory
public class NotEqualComparisonOperatorTest {

	@Test
	@JiraKey( value = "HHH-17234")
	public void testNotEqualComparison(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var entityOne = new IntegerTextMapEntity(1, "One");
					var entityNegativeOne = new IntegerTextMapEntity(-1, "Negative One");
					session.persist(entityOne);
					session.persist(entityNegativeOne);

					var tableContents = session.createQuery(
							"SELECT x FROM IntegerTextMapEntity x", IntegerTextMapEntity.class ).getResultList();
					assertNotNull(tableContents);
					assertEquals(2, tableContents.size());

					var notEqualOpResults = session.createQuery(
							"SELECT x FROM IntegerTextMapEntity x WHERE x.intValue <> -1", IntegerTextMapEntity.class).getResultList();
					assertNotNull(notEqualOpResults);
					assertEquals(1, notEqualOpResults.size());
				}
		);
	}

	@Entity( name = "IntegerTextMapEntity" )
	public static class IntegerTextMapEntity {
		@Id
		@GeneratedValue
		private Long id;

		private Integer intValue;

		private String textValue;

		public IntegerTextMapEntity(int intValue, String textValue) {
			this.intValue = intValue;
			this.textValue = textValue;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getIntValue() {
			return intValue;
		}

		public void setIntValue(Integer intValue) {
			this.intValue = intValue;
		}

		public String getTextValue() {
			return textValue;
		}

		public void setTextValue(String textValue) {
			this.textValue = textValue;
		}
	}

}
