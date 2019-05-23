/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.embeddable;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestNoProxyEntity;
import org.hibernate.envers.test.support.domains.collections.EmbeddableListEntity3;
import org.hibernate.envers.test.support.domains.components.relations.ManyToOneEagerComponent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Cankut Guven
 */
@TestForIssue(jiraKey = "HHH-11364")
public class EmbeddableList3Test extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ele3_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EmbeddableListEntity3.class, StrTestNoProxyEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.ele3_id = inTransaction(
				entityManager -> {
					EmbeddableListEntity3 ele3 = new EmbeddableListEntity3();
					ele3.getComponentList().add( new ManyToOneEagerComponent( null, "data" ) );
					entityManager.persist( ele3 );

					return ele3.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( EmbeddableListEntity3.class, ele3_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testCollectionOfEmbeddableWithNullJoinColumn() {
		final EmbeddableListEntity3 ele3 = getAuditReader().find( EmbeddableListEntity3.class, ele3_id, 1 );
		assertThat( ele3.getComponentList(), CollectionMatchers.hasSize( 1 ) );
	}
}
