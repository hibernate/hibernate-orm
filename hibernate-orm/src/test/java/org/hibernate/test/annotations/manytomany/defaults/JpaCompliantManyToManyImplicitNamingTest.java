/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.manytomany.defaults;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Tests names generated for @JoinTable and @JoinColumn for unidirectional and bidirectional
 * many-to-many associations using the JPA-compliant naming strategy.
 *
 * @author Gail Badner
 */
public class JpaCompliantManyToManyImplicitNamingTest extends ManyToManyImplicitNamingTest {
	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.configureMetadataBuilder( metadataBuilder );
		metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE );
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
		checkDefaultJoinTablAndJoinColumnNames(
				City.class,
				"stolenItems",
				null,
				"tbl_city_ITEM",
				"City_id",
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
		checkDefaultJoinTablAndJoinColumnNames(
				Category.class,
				"clients",
				null,
				"CATEGORY_TAB_KnownClient",
				"CATEGORY_id",
				"clients_id"

		);
	}
}
