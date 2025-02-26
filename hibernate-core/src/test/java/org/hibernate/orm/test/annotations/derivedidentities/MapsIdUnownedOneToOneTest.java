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
import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;

@ServiceRegistry
@Jira("https://hibernate.atlassian.net/browse/HHH-7135")
public class MapsIdUnownedOneToOneTest {

	@Test
	public void testInvalidMapping(ServiceRegistryScope scope) {
		MetadataSources metadataSources = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClasses( MapsIdUnownedOneToOneTest.ThingHolder.class, MapsIdUnownedOneToOneTest.AThing.class, MapsIdUnownedOneToOneTest.Thing1.class, MapsIdUnownedOneToOneTest.Thing2.class );
		try {
			metadataSources.buildMetadata();
			fail( "Was expecting failure" );
		}
		catch (AnnotationException ignore) {
		}
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
		@OneToOne(mappedBy = "thing")
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
