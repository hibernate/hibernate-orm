/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.array;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Date;

@DomainModel(
		annotatedClasses = {
				DateArrayTest.DateArrayEntity.class
		}
)
@SessionFactory
public class DateArrayTest {

	@Test
	public void run(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			DateArrayEntity entity = new DateArrayEntity();
			entity.setDates( new Date[] {new Date()} );
			session.persist( entity );
		} );
	}

	@Entity(name = "DateArrayEntity")
	public static class DateArrayEntity {

		@Id
		@GeneratedValue
		private Long id;

		@ElementCollection
		@OrderColumn
		@Column(name = "dates_column")
		private Date[] dates = new Date[0];

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date[] getDates() {
			return dates;
		}

		public void setDates(Date[] dates) {
			this.dates = dates;
		}
	}

}
