/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


@DomainModel(
		annotatedClasses = {
				MapsIdJoinedInheritanceTest.ThingHolder.class, MapsIdJoinedInheritanceTest.AThing.class, MapsIdJoinedInheritanceTest.Thing1.class, MapsIdJoinedInheritanceTest.Thing2.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-7135")
public class MapsIdJoinedInheritanceTest {

	@Test
	public void testInsertIntoMap(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ThingHolder holder = new ThingHolder();
					Thing1 thing1 = new Thing1( holder, "test" );
					session.persist( holder );
					session.persist( thing1 );
				}
		);

	}

	@Entity(name = "AThing")
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract class AThing {
		public AThing() {
		}

		public AThing(ThingHolder holder) {
			this.holder = holder;
		}

		@Id
		private Integer id;
		@MapsId
		@OneToOne
		private ThingHolder holder;

		public Integer getId() {
			return id;
		}

		public ThingHolder getHolder() {
			return holder;
		}
	}

	@Entity(name = "ThingHolder")
	public class ThingHolder {
		public ThingHolder() {
		}

		@Id
		@GeneratedValue
		private Integer id;
		@OneToOne
		private AThing thing;

		public Integer getId() {
			return id;
		}

		public AThing getThing() {
			return thing;
		}

		public void setThing(AThing thing) {
			this.thing = thing;
		}
	}

	@Entity(name = "Thing1")
	public class Thing1 extends AThing {
		public Thing1() {
			super();
		}

		public Thing1(ThingHolder holder, String string) {
			super( holder );
			this.string = string;
		}

		@Basic
		private String string;

		public String getString() {
			return string;
		}
	}

	@Entity(name = "Thing2")
	public class Thing2 extends AThing {
		public Thing2() {
			super();
		}

		public Thing2(ThingHolder holder, Integer integer) {
			super( holder );
			this.integer = integer;
		}

		@Basic
		private Integer integer;

		public Integer getInteger() {
			return integer;
		}
	}
}
