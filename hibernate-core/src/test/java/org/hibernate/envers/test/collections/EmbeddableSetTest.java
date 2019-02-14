/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.EmbeddableSetEntity;
import org.hibernate.envers.test.support.domains.components.PartialAuditedComponent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class EmbeddableSetTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EmbeddableSetEntity.class };
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-9199")
	public void testRemoval() {
		inJPA( entityManager -> {
			final PartialAuditedComponent comp1 = new PartialAuditedComponent( "comp1", null, null );
			final PartialAuditedComponent comp2 = new PartialAuditedComponent( "comp2", null, null );

			EmbeddableSetEntity entity = new EmbeddableSetEntity();

			entityManager.getTransaction().begin();

			entity.getComponentSet().add( comp1 );
			entity.getComponentSet().add( comp2 );

			entityManager.persist( entity );

			entityManager.getTransaction().commit();

			entityManager.getTransaction().begin();

			entity.getComponentSet().remove( comp1 );

			entityManager.getTransaction().commit();

			EmbeddableSetEntity rev1 = getAuditReader().find( EmbeddableSetEntity.class, entity.getId(), 1 );
			EmbeddableSetEntity rev2 = getAuditReader().find( EmbeddableSetEntity.class, entity.getId(), 2 );
			assertThat( rev1.getComponentSet(), containsInAnyOrder( comp1, comp2 ) );
			assertThat( rev2.getComponentSet(), contains( comp2 ) );
		} );
	}

}
