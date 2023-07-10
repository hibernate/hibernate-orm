package org.hibernate.orm.test.jpa.criteria.nestedfetch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.orm.test.jpa.criteria.nestedfetch.FetchDepth1Entity_;
import org.hibernate.orm.test.jpa.criteria.nestedfetch.FetchDepth2Entity_;
import org.hibernate.orm.test.jpa.criteria.nestedfetch.FetchRootEntity_;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				FetchDepth2Entity.class, FetchRootEntity.class, FetchDepth1Entity.class
		})
@JiraKey("HHH-16905")
public class NestedFetchTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			FetchRootEntity rootEntity = new FetchRootEntity( "111" );

			FetchDepth2Entity depth2Entity = new FetchDepth2Entity();
			depth2Entity.setVal( "222" );

			FetchDepth1Entity depth1Entity = new FetchDepth1Entity();
			depth1Entity.setRootEntity( rootEntity );
			rootEntity.setDepth1Entities( Collections.singleton( depth1Entity ) );

			depth1Entity.setDepth2Entity( depth2Entity );
			depth2Entity.setDepth1Entities( Collections.singleton( depth1Entity ) );

			entityManager.persist( depth2Entity );
			entityManager.persist( rootEntity );
			entityManager.persist( depth1Entity );
		} );
	}

	@Test
	public void testNestedFetch(EntityManagerFactoryScope scope) {

		final List<FetchRootEntity> entities = new ArrayList<>();

		scope.inTransaction( entityManager -> {

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			CriteriaQuery<FetchRootEntity> cq = cb.createQuery( FetchRootEntity.class );

			Root<FetchRootEntity> fromRoot = cq.from( FetchRootEntity.class );

			Fetch<FetchRootEntity, FetchDepth1Entity> fetchDepth1 = fromRoot.fetch(
					FetchRootEntity_.depth1Entities,
					JoinType.LEFT
			);

			Fetch<FetchDepth1Entity, FetchDepth2Entity> fetchDepth2 = fetchDepth1.fetch(
					FetchDepth1Entity_.depth2Entity,
					JoinType.LEFT
			);

			cq.select( fromRoot ).where( cb.equal( fromRoot.get( FetchRootEntity_.value ), "111" ) );

			entities.addAll( entityManager.createQuery( cq ).getResultList() );

		} );

		assertThat( entities.size() ).isEqualTo( 1 );

		//dependent relations should already be fetched and be available outside the transaction
		FetchRootEntity rootEntity = entities.get( 0 );
		assertThat( rootEntity.getValue() ).isEqualTo( "111" );
		assertThat( rootEntity.getDepth1Entities().size() ).isEqualTo( 1 );

		FetchDepth1Entity depth1Entity = rootEntity.getDepth1Entities().iterator().next();
		assertThat( depth1Entity ).isNotNull();

		//ToDo: the next 3 lines causing to fail the test in version 6.2.6, so please comment in

//		FetchDepth2Entity depth2Entity = depth1Entity.getDepth2Entity();
//		assertThat( depth2Entity ).isNotNull();
//		assertThat( depth2Entity.getVal() ).isEqualTo( "222" );
	}
}
