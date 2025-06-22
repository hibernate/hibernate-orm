/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh13151;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {SubEntity.class, SuperEntity.class, SideEntity.class} )
@SessionFactory
@JiraKey( "HHH-13151" )
@SuppressWarnings("JUnitMalformedDeclaration")
public class TreatedEntityFetchTest {
	@Test
	public void testTreatedFetching(SessionFactoryScope sessions) throws Exception {
		final SubEntity result = (SubEntity) sessions.fromTransaction( (session) -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<SuperEntity> criteria = cb.createQuery( SuperEntity.class );
			final Root<SuperEntity> root = criteria.from( SuperEntity.class );
			cb.treat( root, SubEntity.class ).fetch( "subField" );

			return session.createQuery( criteria ).getResultList().get( 0 );
		} );

		final SideEntity subField = result.getSubField();
		assertThat( subField.getName() ).isNotNull();
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.persist( new SubEntity().setSubField( new SideEntity( "testName" ) ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}
}
