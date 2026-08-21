/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-20317")
@EnversTest
@Jpa(annotatedClasses = {
		Named.class,
		DefaultNamed.class
})
class DynamicDiscriminatorTest {

	@BeforeClassTemplate
	void initData(EntityManagerFactoryScope scope) {
		var parent = new DefaultNamed( "Bob", "PERSON" );
		parent.setDescription( "Test" );
		scope.inTransaction( em -> em.persist( parent ) );
	}

	@Test
	void testDiscriminatorOptionsAreReflected(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction( em -> {
			var query = em.createQuery( "from Named where name=?1", Named.class );
			query.setParameter( 1, "Bob" );

			var result = query.getResultList();
			assertThat( result ).hasSize( 1 ).element( 0 ).satisfies( named -> {
				assertThat( named.getName() ).isEqualTo( "Bob" );
				assertThat( named.getType() ).isEqualTo( "PERSON" );
				assertThat( named ).isExactlyInstanceOf( DefaultNamed.class );
			} );

			result.get(0).setDescription( "Updated test" );
		} );

		scope.inTransaction( em -> {
			var revisionReader = AuditReaderFactory.get( em );
			var revisions = revisionReader.getRevisions( Named.class, "Bob" );

			var result = revisionReader.createQuery().forEntitiesAtRevision( Named.class, revisions.get( 0 ) )
					.add( AuditEntity.id().eq( "Bob" ) ).getSingleResult();

			assertThat( result ).isExactlyInstanceOf( DefaultNamed.class ).asInstanceOf( type( DefaultNamed.class ) )
					.satisfies( named -> {
						assertThat( named.getType() ).isEqualTo( "PERSON" );
						assertThat( named.getDescription() ).isEqualTo( "Test" );
					} );
		} );
	}
}
