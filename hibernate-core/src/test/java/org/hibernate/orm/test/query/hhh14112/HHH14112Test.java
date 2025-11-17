/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh14112;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ganesh Tiwari
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-14112")
@DomainModel(
		annotatedClasses = {
				HHH14112Test.Super.class,
				HHH14112Test.SubObject.class
		}
)
@SessionFactory
public class HHH14112Test {

	@Test
	public void testCountSubObjectNotThrownExceptionBecauseOfWhere(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					Long result = session.createQuery( "SELECT count(*) FROM SubObject", Long.class ).getSingleResult();
					assertThat( result ).isEqualTo( 0L );
				}
		);
	}

	@Entity(name = "Super")
	@Inheritance(strategy = InheritanceType.JOINED)
	@SQLRestriction("deleted = false")
	public static class Super {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		public boolean deleted;
	}

	@Entity(name = "SubObject")
	public static class SubObject extends Super {

		public String name;

		public int age;

		public SubObject() {
		}

		public SubObject(String name, int age) {
			this.name = name;
			this.age = age;
		}

	}
}
