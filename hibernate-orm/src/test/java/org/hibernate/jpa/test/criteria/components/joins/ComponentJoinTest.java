/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.components.joins;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ComponentJoinTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entity.class, EmbeddedType.class, ManyToOneType.class };
	}

	public static final String THEVALUE = "thevalue";

	@Before
	public void before() {
		EntityManager entityManager = entityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();
		ManyToOneType manyToOneType = new ManyToOneType( THEVALUE );
		EmbeddedType embeddedType = new EmbeddedType( manyToOneType );
		Entity entity = new Entity( embeddedType );
		entityManager.persist( entity );
		entityManager.getTransaction().commit();
		entityManager.close();
	}
	
	@After
	public void after() {
		EntityManager entityManager = entityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();
		entityManager.createQuery( "delete Entity" ).executeUpdate();
		entityManager.createQuery( "delete ManyToOneType" ).executeUpdate();
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	interface JoinBuilder {
		Join<EmbeddedType,ManyToOneType> buildJoinToManyToOneType(Join<Entity, EmbeddedType> source);
	}

	private void doTest(JoinBuilder joinBuilder) {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

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

		assertEquals( THEVALUE, result.get(0) );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void getResultWithStringPropertyDerivedPath() {
		doTest(
				new JoinBuilder() {
					@Override
					public Join<EmbeddedType, ManyToOneType> buildJoinToManyToOneType(Join<Entity, EmbeddedType> source) {
						return source.join( "manyToOneType", JoinType.LEFT );
					}
				}
		);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void getResultWithMetamodelDerivedPath() {
		doTest(
				new JoinBuilder() {
					@Override
					public Join<EmbeddedType, ManyToOneType> buildJoinToManyToOneType(Join<Entity, EmbeddedType> source) {
						final SingularAttribute<EmbeddedType, ManyToOneType> attr =
								(SingularAttribute<EmbeddedType, ManyToOneType>) entityManagerFactory().getMetamodel()
										.managedType( EmbeddedType.class )
										.getDeclaredSingularAttribute( "manyToOneType" );
						return source.join( attr, JoinType.LEFT );
					}
				}
		);
	}
}
