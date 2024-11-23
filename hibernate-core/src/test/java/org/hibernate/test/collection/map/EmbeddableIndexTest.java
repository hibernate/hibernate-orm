/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.map;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class EmbeddableIndexTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TheOne.class, TheMany.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					TheOne one = new TheOne( "1" );
					session.save( one );

					TheMapKey theMapKey = new TheMapKey( one );
					TheMany theMany = new TheMany( theMapKey );
					session.save( theMany );

					Map<TheMapKey, TheMany> map = new HashMap<>();
					map.put( theMapKey, theMany );
					one.setTheManys( map );
				}
		);
	}

	@Test
	public void testIt() {
		inSession(
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
