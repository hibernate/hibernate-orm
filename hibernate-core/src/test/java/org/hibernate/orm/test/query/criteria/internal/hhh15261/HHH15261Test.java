package org.hibernate.orm.test.query.criteria.internal.hhh15261;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@TestForIssue(jiraKey = "HHH-15261")
@Jpa(
		annotatedClasses = { EntityA.class, EntityB.class, EntityC.class }
)
public class HHH15261Test {
	
	@Test
	public void testJoinOnSelection(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<EntityB> query = builder.createQuery( EntityB.class );
			
			final Root<EntityA> root = query.from( EntityA.class );
			root.join( "b" )
					.fetch( "c" );
			query.select( root.get( "b" ) );

			assertDoesNotThrow( () -> entityManager.createQuery( query ).getResultList() );
		} );
	}

}
