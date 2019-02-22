/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.mapkey;

import java.util.Arrays;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.mapkey.ComponentMapKeyEntity;
import org.hibernate.envers.test.support.domains.components.Component1;
import org.hibernate.envers.test.support.domains.components.Component2;
import org.hibernate.envers.test.support.domains.components.ComponentTestEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ComponentMapKeyTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer cmke_id;

	private Integer cte1_id;
	private Integer cte2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentMapKeyEntity.class, ComponentTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - Maxmimum fetch depth handling seems to be problematic with ValidityAuditStrategy.
		//		At line 92, this test would fail without the following configuration property because the
		//		navigableReferenceStack depth is 7 which exceeds the maximumDepth default of 5.
		//		This lead to the sqlSelections not being resolved and therefore a select-clause that had
		//		absolutely no selectables; thus a SQL syntax exception.
		//

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (initially 1 mapping)
				entityManager -> {
					final ComponentTestEntity cte1 = new ComponentTestEntity(
							new Component1( "x1", "y2" ),
							new Component2( "a1", "b2" )
					);

					final ComponentTestEntity cte2 = new ComponentTestEntity(
							new Component1( "x1", "y2" ),
							new Component2( "a1", "b2" )
					);

					entityManager.persist( cte1 );
					entityManager.persist( cte2 );

					this.cte1_id = cte1.getId();
					this.cte2_id = cte2.getId();

					final ComponentMapKeyEntity imke = new ComponentMapKeyEntity();
					imke.getIdmap().put( cte1.getComp1(), cte1 );
					entityManager.persist( imke );

					this.cmke_id = imke.getId();
				},

				// Revision 2 (sse1: adding 1 mapping)
				entityManager -> {
					final ComponentTestEntity cte2 = entityManager.find( ComponentTestEntity.class, cte2_id );
					final ComponentMapKeyEntity imke = entityManager.find( ComponentMapKeyEntity.class, cmke_id );

					// This technically replaces the reference of cte1 with Comp1 with cte2.
					imke.getIdmap().put( cte2.getComp1(), cte2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ComponentMapKeyEntity.class, cmke_id ) );
	}

	@DynamicTest
	public void testHistoryOfImke() {
		ComponentTestEntity cte1 = getEntityManager().find( ComponentTestEntity.class, cte1_id );
		ComponentTestEntity cte2 = getEntityManager().find( ComponentTestEntity.class, cte2_id );

		// These fields are unversioned.
		cte1.setComp2( null );
		cte2.setComp2( null );

		ComponentMapKeyEntity rev1 = getAuditReader().find( ComponentMapKeyEntity.class, cmke_id, 1 );
		ComponentMapKeyEntity rev2 = getAuditReader().find( ComponentMapKeyEntity.class, cmke_id, 2 );

		assertThat( rev1.getIdmap().entrySet(), CollectionMatchers.hasSize( 1 ) );
		assertThat( rev1.getIdmap(), hasEntry( cte1.getComp1(), cte1 ) );

		assertThat( rev2.getIdmap().entrySet(), CollectionMatchers.hasSize( 1 ) );
		assertThat( rev2.getIdmap(), hasEntry( cte2.getComp1(), cte2 ) );
	}
}