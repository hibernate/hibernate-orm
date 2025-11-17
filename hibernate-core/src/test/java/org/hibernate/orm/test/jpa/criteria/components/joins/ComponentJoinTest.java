/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.components.joins;

import java.util.List;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {
		Entity.class,
		EmbeddedType.class,
		ManyToOneType.class
})
public class ComponentJoinTest {

	public static final String THEVALUE = "thevalue";

	@BeforeEach
	public void before(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ManyToOneType manyToOneType = new ManyToOneType( THEVALUE );
					EmbeddedType embeddedType = new EmbeddedType( manyToOneType );
					Entity entity = new Entity( embeddedType );
					entityManager.persist( entity );
				}
		);
	}

	@AfterEach
	public void after(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	private void doTest(EntityManagerFactoryScope scope, JoinBuilder joinBuilder) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Tuple> criteriaQuery = builder.createTupleQuery();
					Root<Entity> root = criteriaQuery.from( Entity.class );
					Join<Entity, EmbeddedType> join = root.join( "embeddedType", JoinType.LEFT );

					// left join to the manyToOne on the embeddable with a string property
					Path<String> path = joinBuilder.buildJoinToManyToOneType( join ).get( "value" );

					// select the path in the tuple
					criteriaQuery.select( builder.tuple( path ) );

					List<Tuple> results = entityManager.createQuery( criteriaQuery ).getResultList();
					Tuple result = results.iterator().next();

					assertEquals( THEVALUE, result.get( 0 ) );
				}
		);
	}

	@Test
	public void getResultWithStringPropertyDerivedPath(EntityManagerFactoryScope scope) {
		doTest( scope,
				source -> source.join( "manyToOneType", JoinType.LEFT )
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getResultWithMetamodelDerivedPath(EntityManagerFactoryScope scope) {
		doTest( scope,
				source -> {
					final SingularAttribute<EmbeddedType, ManyToOneType> attr =
							(SingularAttribute<EmbeddedType, ManyToOneType>) scope.getEntityManagerFactory().getMetamodel()
									.managedType( EmbeddedType.class )
									.getDeclaredSingularAttribute( "manyToOneType" );
					return source.join( attr, JoinType.LEFT );
				}
		);
	}

	interface JoinBuilder {
		Join<EmbeddedType, ManyToOneType> buildJoinToManyToOneType(Join<Entity, EmbeddedType> source);
	}
}
