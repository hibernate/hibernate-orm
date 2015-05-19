/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;

/**
 * HHH-5715 bug test case: Duplicated entries when using select distinct with join and pagination. The bug has to do
 * with new {@link SQLServerDialect} that uses row_number function for pagination
 * 
 * @author Valotasios Yoryos
 */
@TestForIssue( jiraKey = "HHH-5715" )
public class DistinctSelectTest extends BaseCoreFunctionalTestCase {
	private static final int NUM_OF_USERS = 30;

	@Override
	public String[] getMappings() {
		return new String[] { "pagination/EntryTag.hbm.xml" };
	}

	public void feedDatabase() {
		List<Tag> tags = new ArrayList<Tag>();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		for (int i = 0; i < 5; i++) {
			Tag tag = new Tag("Tag: " + UUID.randomUUID().toString());
			tags.add(tag);
			s.save(tag);
		}

		for (int i = 0; i < NUM_OF_USERS; i++) {
			Entry e = new Entry("Entry: " + UUID.randomUUID().toString());
			e.getTags().addAll(tags);
			s.save(e);
		}
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDistinctSelectWithJoin() {
		feedDatabase();

		Session s = openSession();

		List<Entry> entries = s.createQuery("select distinct e from Entry e join e.tags t where t.surrogate != null order by e.name").setFirstResult(10).setMaxResults(5).list();

		// System.out.println(entries);
		Entry firstEntry = entries.remove(0);
		assertFalse("The list of entries should not contain dublicated Entry objects as we've done a distinct select", entries.contains(firstEntry));

		s.close();
	}
}
