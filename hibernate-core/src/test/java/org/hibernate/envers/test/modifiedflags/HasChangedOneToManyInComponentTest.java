/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.components.relations.OneToManyComponent;
import org.hibernate.envers.test.support.domains.components.relations.OneToManyComponentTestEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedOneToManyInComponentTest extends AbstractModifiedFlagsEntityTest {

	private Integer otmcte_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { OneToManyComponentTestEntity.class, StrTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		//		This is currently a workaround to get the test to pass.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		StrTestEntity ste1 = new StrTestEntity();
		ste1.setStr( "str1" );

		StrTestEntity ste2 = new StrTestEntity();
		ste2.setStr( "str2" );

		inTransactions(
				// Revision 1
				entityManager -> {
					entityManager.persist( ste1 );
					entityManager.persist( ste2 );
				},

				// Revision 2
				entityManager -> {
					final OneToManyComponent component = new OneToManyComponent( "data1" );
					final OneToManyComponentTestEntity otmcte1 = new OneToManyComponentTestEntity( component );
					otmcte1.getComp1().getEntities().add( ste1 );

					entityManager.persist( otmcte1 );

					otmcte_id1 = otmcte1.getId();
				},

				// Revision 3
				entityManager -> {
					OneToManyComponentTestEntity otmcte1 = entityManager.find( OneToManyComponentTestEntity.class, otmcte_id1 );
					otmcte1.getComp1().getEntities().add( ste2 );
				}
		);
	}

	@DynamicTest
	public void testHasChangedId1() {
		assertThat(
				extractRevisions( queryForPropertyHasChanged( OneToManyComponentTestEntity.class, otmcte_id1, "comp1" ) ),
				contains( 2, 3 )
		);

		assertThat(
				queryForPropertyHasNotChanged( OneToManyComponentTestEntity.class, otmcte_id1, "comp1" ),
				CollectionMatchers.isEmpty()
		);
	}
}