/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.unidirectional;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.ids.EmbIdTestEntity;
import org.hibernate.envers.test.support.domains.onetoone.unidirectional.UniRefIngMulIdEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class UnidirectionalMulIdWithNullsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private EmbId ei;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EmbIdTestEntity.class, UniRefIngMulIdEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					ei = new EmbId( 1, 2 );

					EmbIdTestEntity eite = new EmbIdTestEntity( ei, "data" );
					UniRefIngMulIdEntity notNullRef = new UniRefIngMulIdEntity( 1, "data 1", eite );
					UniRefIngMulIdEntity nullRef = new UniRefIngMulIdEntity( 2, "data 2", null );

					entityManager.persist( eite );
					entityManager.persist( notNullRef );
					entityManager.persist( nullRef );
				}
		);
	}

	@DynamicTest
	public void testNullReference() {
		assertThat( getAuditReader().find( UniRefIngMulIdEntity.class, 2, 1 ).getReference(), nullValue() );
	}

	@DynamicTest
	public void testNotNullReference() {
		final EmbIdTestEntity eite = getAuditReader().find( EmbIdTestEntity.class, ei, 1 );
		assertThat( getAuditReader().find( UniRefIngMulIdEntity.class, 1, 1 ).getReference(), equalTo( eite ) );
	}
}