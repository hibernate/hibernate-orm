/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.mappedsuperclass;

import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.mappedsuperclass.AbstractEmbeddable;
import org.hibernate.envers.test.support.domains.components.mappedsuperclass.EmbeddableWithNoDeclaredData;
import org.hibernate.envers.test.support.domains.components.mappedsuperclass.EntityWithEmbeddableWithNoDeclaredData;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-9193")
public class EmbeddableWithNoDeclaredDataTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithEmbeddableWithNoDeclaredData.class, AbstractEmbeddable.class, EmbeddableWithNoDeclaredData.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				entityManager -> {
					EntityWithEmbeddableWithNoDeclaredData entity = new EntityWithEmbeddableWithNoDeclaredData();
					entity.setName( "Entity 1" );
					entity.setValue( new EmbeddableWithNoDeclaredData( 84 ) );

					entityManager.persist( entity );
					return entity.getId();
				}
		);
	}

	@DynamicTest
	public void testEmbeddableThatExtendsMappedSuperclass() {
		final EntityWithEmbeddableWithNoDeclaredData entityLoaded = inTransaction(
				entityManager -> {
					return entityManager.find( EntityWithEmbeddableWithNoDeclaredData.class, id );
				}
		);

		final AuditReader reader = getAuditReader();

		List<Number> revs = getAuditReader().getRevisions( EntityWithEmbeddableWithNoDeclaredData.class, id );
		assertThat( revs, CollectionMatchers.hasSize( 1 ) );

		EntityWithEmbeddableWithNoDeclaredData entityRev1 = reader.find( EntityWithEmbeddableWithNoDeclaredData.class, id, revs.get( 0 ) );
		assertThat( entityRev1.getName(), equalTo( entityLoaded.getName() ) );

		// value should be null because there is no data in EmbeddableWithNoDeclaredData
		// and the fields in AbstractEmbeddable should not be audited.
		assertThat( entityRev1.getValue(), nullValue() );
	}
}
