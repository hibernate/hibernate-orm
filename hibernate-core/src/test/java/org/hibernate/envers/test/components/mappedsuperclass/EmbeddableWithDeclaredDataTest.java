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
import org.hibernate.envers.test.support.domains.components.mappedsuperclass.EmbeddableWithDeclaredData;
import org.hibernate.envers.test.support.domains.components.mappedsuperclass.EntityWithEmbeddableWithDeclaredData;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;
import org.hibernate.testing.orm.junit.FailureExpected;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-9193")
public class EmbeddableWithDeclaredDataTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithEmbeddableWithDeclaredData.class, AbstractEmbeddable.class, EmbeddableWithDeclaredData.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				entityManager -> {
					final EntityWithEmbeddableWithDeclaredData entity = new EntityWithEmbeddableWithDeclaredData();
					entity.setName( "Entity 1" );
					entity.setValue( new EmbeddableWithDeclaredData( 42, "TestCodeart" ) );

					entityManager.persist( entity );

					return entity.getId();
				}
		);
	}

	@DynamicTest
	@FailureExpected(jiraKey = "HHH-9193")
	public void testEmbeddableThatExtendsMappedSuperclass() {
		// Reload and Compare Revision
		final EntityWithEmbeddableWithDeclaredData entityLoaded = inTransaction(
				entityManager -> {
					return entityManager.find( EntityWithEmbeddableWithDeclaredData.class, id );
				}
		);

		final AuditReader reader = getAuditReader();

		List<Number> revs = reader.getRevisions( EntityWithEmbeddableWithDeclaredData.class, id );
		assertThat( revs, CollectionMatchers.hasSize( 1 ) );

		EntityWithEmbeddableWithDeclaredData entityRev1 = reader.find( EntityWithEmbeddableWithDeclaredData.class, id, revs.get( 0 ) );
		assertThat( entityRev1.getName(), equalTo( entityLoaded.getName() ) );

		// only value.codeArt should be audited because it is the only audited field in EmbeddableWithDeclaredData;
		// fields in AbstractEmbeddable should not be audited.
		assertThat( entityRev1.getValue().getCodeart(), equalTo( entityLoaded.getValue().getCodeart() ) );
		assertThat( entityRev1.getValue().getCode(), nullValue() );
	}
}
