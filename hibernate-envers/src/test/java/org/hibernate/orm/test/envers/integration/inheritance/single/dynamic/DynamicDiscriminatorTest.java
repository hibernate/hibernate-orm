/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

@JiraKey(value = "HHH-20317")
public class DynamicDiscriminatorTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Named.class, DefaultNamed.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - persist
		em.getTransaction().begin();
		var parent = new DefaultNamed( "Bob", "PERSON" );
		parent.setDescription( "Test" );
		em.persist( parent );
		em.getTransaction().commit();

		// Revision 2 - update
		em.getTransaction().begin();
		var found = em.find( DefaultNamed.class, "Bob" );
		found.setDescription( "Updated test" );
		em.getTransaction().commit();
	}

	@Test
	public void testDiscriminatorOptionsAreReflected() {
		var revisions = getAuditReader().getRevisions( Named.class, "Bob" );
		assertThat( revisions ).hasSize( 2 );

		var result = getAuditReader().createQuery().forEntitiesAtRevision( Named.class, revisions.get( 0 ) )
				.add( AuditEntity.id().eq( "Bob" ) ).getSingleResult();

		assertThat( result ).isExactlyInstanceOf( DefaultNamed.class ).asInstanceOf( type( DefaultNamed.class ) )
				.satisfies( named -> {
					assertThat( named.getType() ).isEqualTo( "PERSON" );
					assertThat( named.getDescription() ).isEqualTo( "Test" );
				} );
	}
}
