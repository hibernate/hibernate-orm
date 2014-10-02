/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.manytomany.defaults;

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.naming.LegacyNamingStrategyDelegator;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;

/**
 * @author Gail Badner
 */
public class LegacyManyToManyDefaultsTest extends ManyToManyDefaultsTest {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategyDelegator( new LegacyNamingStrategyDelegator( EJB3NamingStrategy.INSTANCE ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9390")
	public void testUnidirOwnerPrimaryTableAssocEntityNamePKOverride() {
		// City.stolenItems; associated entity: Item
		// City has @Entity with no name configured and @Table(name = "tbl_city")
		// Item has @Entity(name="ITEM") and no @Table
		// PK column for City.id: id (default)
		// PK column for Item: iId
		// unidirectional
		// legacy behavior would use the table name in the generated join column.
		checkDefaultJoinTablAndJoinColumnNames(
				City.class,
				"stolenItems",
				null,
				"tbl_city_ITEM",
				"tbl_city_id",
				"stolenItems_iId"
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9390")
	public void testUnidirOwnerEntityNamePrimaryTableOverride() {
		// Category.clients: associated entity: KnownClient
		// Category has @Entity(name="CATEGORY") @Table(name="CATEGORY_TAB")
		// KnownClient has @Entity with no name configured and no @Table
		// PK column for Category.id: id (default)
		// PK column for KnownClient.id: id (default)
		// unidirectional
		// legacy behavior would use the table name in the generated join column.
		checkDefaultJoinTablAndJoinColumnNames(
				Category.class,
				"clients",
				null,
				"CATEGORY_TAB_KnownClient",
				"CATEGORY_TAB_id",
				"clients_id"

		);
	}
}
