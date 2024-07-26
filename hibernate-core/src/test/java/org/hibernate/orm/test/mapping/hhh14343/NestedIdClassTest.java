/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.hhh14343;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy;

import org.hibernate.orm.test.mapping.hhh14343.entity.NestedPlayerStat;
import org.hibernate.orm.test.mapping.hhh14343.entity.NestedScore;
import org.hibernate.orm.test.mapping.hhh14343.entity.NestedStat;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@JiraKey(value = "HHH-14343")
public class NestedIdClassTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				NestedStat.class,
				NestedPlayerStat.class,
				NestedScore.class
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
	public void testNestedIdClasses() {
		doInJPA( this::entityManagerFactory, em ->
		{
			// do nothing
		} );
	}
}
