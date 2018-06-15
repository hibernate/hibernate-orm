/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetomany2;

import java.util.HashSet;
import java.util.List;

import org.hibernate.test.onetomany2.Child;
import org.hibernate.test.onetomany2.Parent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author pholvs
 */
public class WhereTest extends BaseCoreFunctionalTestCase {

	static Parent p = null;
	public String[] getMappings() {
		return new String[] { "onetomany2/mappings.hbm.xml" };
	}

	@Before
	public void createTestData() {
		inTransaction(
				s -> {
					p = new Parent(99999L);
					s.save(p);

					Child c = new Child(p);
					p.getChildren().add(c);
					s.save(c);
				}
		);
	}


	@Test
	public void testHqlWithFetch() {
		inTransaction(
				s -> {
					final Parent p2 = s.load(Parent.class, p.getId());
					assertEquals( 1, p2.getChildren().size() );
				}
		);
	}
}

