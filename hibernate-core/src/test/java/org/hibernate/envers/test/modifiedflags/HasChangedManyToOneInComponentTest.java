/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.components.relations.ManyToOneComponent;
import org.hibernate.envers.test.support.domains.components.relations.ManyToOneComponentTestEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedManyToOneInComponentTest extends AbstractModifiedFlagsEntityTest {
	private Integer entityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ManyToOneComponentTestEntity.class, StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final StrTestEntity ste1 = new StrTestEntity();
		ste1.setStr( "str1" );

		final StrTestEntity ste2 = new StrTestEntity();
		ste2.setStr( "str2" );

		inTransactions(
				// Revision 1
				entityManager -> {
					entityManager.persist( ste1 );
					entityManager.persist( ste2 );
				},

				// Revision 2
				entityManager -> {
					final ManyToOneComponent component = new ManyToOneComponent( ste1, "data1" );
					final ManyToOneComponentTestEntity entity = new ManyToOneComponentTestEntity( component );

					entityManager.persist( entity );
					this.entityId = entity.getId();
				},

				// Revision 3
				entityManager -> {
					entityManager.find( ManyToOneComponentTestEntity.class, entityId ).getComp1().setEntity( ste2 );
				}
		);
	}

	@DynamicTest
	public void testHasChangedId1() {
		assertThat(
				extractRevisions( queryForPropertyHasChanged( ManyToOneComponentTestEntity.class, entityId, "comp1" ) ),
				contains( 2, 3 )
		);

		assertThat(
				queryForPropertyHasNotChanged( ManyToOneComponentTestEntity.class, entityId, "comp1" ),
				CollectionMatchers.isEmpty()
		);
	}

}
