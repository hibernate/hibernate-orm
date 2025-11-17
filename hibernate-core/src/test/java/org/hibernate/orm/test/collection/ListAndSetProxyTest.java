/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = {
				ListAndSetProxyTest.TheOne.class,
				ListAndSetProxyTest.TheMany.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-11801")
public class ListAndSetProxyTest {

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
					one.getTheManyList().add( theMany );
				}
		);
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					TheOne one = session.find( TheOne.class, "1" );

					Set<TheMapKey> set1 = one.getTheManys().keySet();
					Set<TheMapKey> set2 = one.getTheManys().keySet();
					assertThat( set1, is( equalTo( set2 ) ) );
					assertThat( set1, is( not( sameInstance( set2 ) ) ) );

					List<TheMany> list1 = one.getTheManyList().subList( 0, 1 );
					List<TheMany> list2 = one.getTheManyList().subList( 0, 1 );
					assertThat( list1, is( equalTo( list2 ) ) );
					assertThat( list1, is( not( sameInstance( list2 ) ) ) );
				}
		);
	}

	@Entity(name = "TheOne")
	public static class TheOne {
		private String id;
		private String aString;
		private Map<TheMapKey, TheMany> theManys = new HashMap<>();
		private List<TheMany> theManyList = new ArrayList<>();

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

		@OneToMany
		@JoinTable
		public List<TheMany> getTheManyList() {
			return theManyList;
		}

		public void setTheManyList(List<TheMany> theManyList) {
			this.theManyList = theManyList;
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
