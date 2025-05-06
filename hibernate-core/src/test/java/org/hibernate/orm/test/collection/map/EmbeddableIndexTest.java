/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddableIndexTest.TheOne.class,
				EmbeddableIndexTest.TheMany.class
		}
)
@SessionFactory
public class EmbeddableIndexTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TheOne one = new TheOne( "1" );
					session.persist( one );

					TheMapKey theMapKey = new TheMapKey( one );
					TheMany theMany = new TheMany( theMapKey );
					session.persist( theMany );

					Map<TheMapKey, TheMany> map = new HashMap<>();
					map.put( theMapKey, theMany );
					one.setTheManys( map );
				}
		);
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					TheOne one = session.get( TheOne.class, "1" );
					TheMapKey theMapKey = one.getTheManys().keySet().iterator().next();
					assertThat( theMapKey, is( notNullValue() ) );
					assertThat( theMapKey.getTheOne(), sameInstance( one ) );
				}
		);
	}

	@Entity(name = "TheOne")
	public static class TheOne {
		private String id;
		private String aString;
		private Map<TheMapKey, TheMany> theManys = new HashMap<>();

		TheOne() {
		}

		public TheOne(String id) {
			this.id = id;
		}

		@Id
		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@OneToMany(mappedBy = "theMapKey.theOne")
		@MapKey(name = "theMapKey")
		public Map<TheMapKey, TheMany> getTheManys() {
			return theManys;
		}

		public void setTheManys(Map<TheMapKey, TheMany> theManys) {
			this.theManys = theManys;
		}

		public String getaString() {
			return aString;
		}

		public void setaString(String aString) {
			this.aString = aString;
		}
	}

	@Embeddable
	public static class TheMapKey implements Serializable {
		private TheOne theOne;
		private int anInt;

		TheMapKey() {
		}

		public TheMapKey(TheOne theOne) {
			this.theOne = theOne;
		}

		@ManyToOne
		public TheOne getTheOne() {
			return theOne;
		}

		public void setTheOne(TheOne theOne) {
			this.theOne = theOne;
		}

		public int getAnInt() {
			return anInt;
		}

		public void setAnInt(int anInt) {
			this.anInt = anInt;
		}
	}

	@Entity(name = "TheMany")
	public static class TheMany {
		private TheMapKey theMapKey;
		private String aString;

		TheMany() {
		}

		public TheMany(TheMapKey theMapKey) {
			this.theMapKey = theMapKey;
		}

		@EmbeddedId
		public TheMapKey getTheMapKey() {
			return theMapKey;
		}

		public void setTheMapKey(TheMapKey theMapKey) {
			this.theMapKey = theMapKey;
		}

		public String getaString() {
			return aString;
		}

		public void setaString(String aString) {
			this.aString = aString;
		}
	}
}
