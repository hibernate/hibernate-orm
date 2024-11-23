/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh14276;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy;

import org.hibernate.orm.test.mapping.hhh14276.entity.PlayerStat;
import org.hibernate.orm.test.mapping.hhh14276.entity.Score;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

@JiraKey(value = "HHH-14276")
public class NestedIdClassDerivedIdentifiersTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				PlayerStat.class,
				Score.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE );
		options.put( AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY, InlineMutationStrategy.class.getName() );
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, em ->
		{
			// do nothing
		} );
	}

	@Test
	public void testNestedIdClassDerivedIdentifiers() {
		doInJPA( this::entityManagerFactory, em ->
		{
			// do nothing
		} );
	}
}
