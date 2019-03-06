/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.notupdatable;

import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.notupdatable.PropertyNotUpdatableEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-5411")
@Disabled("DefaultRefreshEventListener does not update the entity instance in the event but creates a new instance; therefore propagated state differs between 5.x and 6.0.")
public class PropertyNotUpdatableTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { PropertyNotUpdatableEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					PropertyNotUpdatableEntity entity = new PropertyNotUpdatableEntity(
							"data",
							"constant data 1",
							"constant data 2"
					);
					entityManager.persist( entity );

					id = entity.getId();
				},

				// Revision 2
				entityManager -> {
					PropertyNotUpdatableEntity entity = entityManager.find( PropertyNotUpdatableEntity.class, id );
					entity.setData( "modified data" );
					entity.setConstantData1( null );
					entityManager.merge( entity );
				}
		);

		// NOTE: Purposely create a new EntityManager instance to re-initialize fields with db values
		// that are not updated. Otherwise PostUpdateEvent#getOldState() returns previous memory state.
		// This can be achieved also by using EntityManager#refresh(Object) as well.
		inJPA(
				entityManager -> {
					// Revision 3
					entityManager.getTransaction().begin();
					PropertyNotUpdatableEntity entity = entityManager.find( PropertyNotUpdatableEntity.class, id );
					entity.setData( "another modified data" );
					entity.setConstantData2( "invalid data" );
					entityManager.merge( entity );
					entityManager.getTransaction().commit();

					// Revision 4
					entityManager.getTransaction().begin();
					entityManager.refresh( entity );
					entityManager.remove( entity );
					entityManager.getTransaction().commit();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( PropertyNotUpdatableEntity.class, id), contains( 1, 2, 3, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfId() {
		PropertyNotUpdatableEntity ver1 = new PropertyNotUpdatableEntity(
				"data",
				"constant data 1",
				"constant data 2",
				id
		);
		assertThat( getAuditReader().find( PropertyNotUpdatableEntity.class, id, 1 ), equalTo( ver1 ) );

		PropertyNotUpdatableEntity ver2 = new PropertyNotUpdatableEntity(
				"modified data",
				"constant data 1",
				"constant data 2",
				id
		);
		assertThat( getAuditReader().find( PropertyNotUpdatableEntity.class, id, 2), equalTo( ver2 ) );

		PropertyNotUpdatableEntity ver3 = new PropertyNotUpdatableEntity(
				"another modified data",
				"constant data 1",
				"constant data 2",
				id
		);
		assertThat( getAuditReader().find( PropertyNotUpdatableEntity.class, id, 3 ), equalTo( ver3 ) );
	}

	@DynamicTest
	public void testDeleteState() {
		final PropertyNotUpdatableEntity delete = new PropertyNotUpdatableEntity(
				"another modified data",
				"constant data 1",
				"constant data 2",
				id
		);

		@SuppressWarnings("unchecked")
		List<Object> results = getAuditReader().createQuery()
				.forRevisionsOfEntity( PropertyNotUpdatableEntity.class, true, true )
				.getResultList();
		assertThat( results.get( 3 ), equalTo( delete ) );
	}
}
