/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;

/**
 * HHH-5715 bug test case: Duplicated entries when using select distinct with join and pagination. The bug has to do
 * with new {@link SQLServerDialect} that uses row_number function for pagination
 *
 * @author Valotasios Yoryos
 */
@JiraKey( value = "HHH-5715" )
public class DistinctSelectTest extends BaseCoreFunctionalTestCase {
	private static final int NUM_OF_USERS = 30;

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "pagination/EntryTag.hbm.xml" };
	}

	public void feedDatabase() {
		List<Tag> tags = new ArrayList<Tag>();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		for (int i = 0; i < 5; i++) {
			Tag tag = new Tag("Tag: " + SafeRandomUUIDGenerator.safeRandomUUID());
			tags.add(tag);
			s.persist(tag);
		}

		for (int i = 0; i < NUM_OF_USERS; i++) {
			Entry e = new Entry("Entry: " + SafeRandomUUIDGenerator.safeRandomUUID());
			e.getTags().addAll(tags);
			s.persist(e);
		}
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testDistinctSelectWithJoin() {
		feedDatabase();

		Session s = openSession();

		List<Entry> entries = s.createQuery("select distinct e from Entry e join e.tags t where t.surrogate is not null order by e.name").setFirstResult(10).setMaxResults(5).list();

		// System.out.println(entries);
		Entry firstEntry = entries.remove(0);
		assertFalse("The list of entries should not contain dublicated Entry objects as we've done a distinct select", entries.contains(firstEntry));

		s.close();
	}
}
