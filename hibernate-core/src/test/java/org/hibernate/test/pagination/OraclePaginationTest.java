package org.hibernate.test.pagination;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(Oracle9iDialect.class)
public class OraclePaginationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				RootEntity.class,
		};
	}

	@Entity(name = "RootEntity")
	@Table(name = "V_MYTABLE_LAST")
	public static class RootEntity implements Serializable {

		@Id
		private Long id;

		@Id
		private Long version;

		private String caption;

		private Long status;

		public RootEntity() {
		}

		public RootEntity(Long id, Long version, String caption, Long status) {
			this.id = id;
			this.version = version;
			this.caption = caption;
			this.status = status;
		}

		public Long getId() {
			return id;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12087")
	public void testPagination() throws Exception {

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new RootEntity( 1L, 7L, "t40", 2L ) );
			entityManager.persist( new RootEntity( 16L, 1L, "t47", 2L ) );
			entityManager.persist( new RootEntity( 11L, 2L, "t43", 2L ) );
			entityManager.persist( new RootEntity( 6L, 4L, "t31", 2L ) );
			entityManager.persist( new RootEntity( 15L, 1L, "t46", 2L ) );
			entityManager.persist( new RootEntity( 2L, 6L, "t39", 2L ) );
			entityManager.persist( new RootEntity( 14L, 1L, "t45", 2L ) );
			entityManager.persist( new RootEntity( 4L, 5L, "t38", 2L ) );
			entityManager.persist( new RootEntity( 8L, 2L, "t29", 2L ) );
			entityManager.persist( new RootEntity( 17L, 1L, "t48", 2L ) );
			entityManager.persist( new RootEntity( 3L, 3L, "t21", 2L ) );
			entityManager.persist( new RootEntity( 7L, 2L, "t23", 2L ) );
			entityManager.persist( new RootEntity( 9L, 2L, "t30", 2L ) );
			entityManager.persist( new RootEntity( 10L, 3L, "t42", 2L ) );
			entityManager.persist( new RootEntity( 12L, 1L, "t41", 2L ) );
			entityManager.persist( new RootEntity( 5L, 6L, "t37", 1L ) );
			entityManager.persist( new RootEntity( 13L, 1L, "t44", 1L ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<RootEntity> rootEntitiesAllPages = getLimitedRows( entityManager, 0, 10 );

			List<RootEntity> rootEntitiesFirst = getLimitedRows( entityManager, 0, 5 );
			List<RootEntity> rootEntitiesSecond = getLimitedRows( entityManager, 5, 10 );

			assertEquals( rootEntitiesAllPages.get( 0 ).getId(), rootEntitiesFirst.get( 0 ).getId() );
			assertEquals( rootEntitiesAllPages.get( 1 ).getId(), rootEntitiesFirst.get( 1 ).getId() );
			assertEquals( rootEntitiesAllPages.get( 2 ).getId(), rootEntitiesFirst.get( 2 ).getId() );
			assertEquals( rootEntitiesAllPages.get( 3 ).getId(), rootEntitiesFirst.get( 3 ).getId() );
			assertEquals( rootEntitiesAllPages.get( 4 ).getId(), rootEntitiesFirst.get( 4 ).getId() );

			assertEquals( rootEntitiesAllPages.get( 5 ).getId(), rootEntitiesSecond.get( 0 ).getId() );
			assertEquals( rootEntitiesAllPages.get( 6 ).getId(), rootEntitiesSecond.get( 1 ).getId() );
			assertEquals( rootEntitiesAllPages.get( 7 ).getId(), rootEntitiesSecond.get( 2 ).getId() );
			assertEquals( rootEntitiesAllPages.get( 8 ).getId(), rootEntitiesSecond.get( 3 ).getId() );
			assertEquals( rootEntitiesAllPages.get( 9 ).getId(), rootEntitiesSecond.get( 4 ).getId() );
		} );
	}

	private List<RootEntity> getAllRows(EntityManager em) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<RootEntity> cq = cb.createQuery( RootEntity.class );
		Root<RootEntity> c = cq.from( RootEntity.class );
		return em.createQuery( cq.select( c ).orderBy( cb.desc( c.get( "status" ) ) ) ).getResultList();
	}

	private List<RootEntity> getLimitedRows(EntityManager em, int start, int end) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<RootEntity> cq = cb.createQuery( RootEntity.class );
		Root<RootEntity> c = cq.from( RootEntity.class );
		CriteriaQuery<RootEntity> select = cq.select( c ).orderBy( cb.desc( c.get( "status" ) ) );
		TypedQuery<RootEntity> typedQuery = em.createQuery( select );
		typedQuery.setFirstResult( start );
		typedQuery.setMaxResults( end );
		return typedQuery.getResultList();
	}

	private void createRootEntity(EntityManager entityManager, Long id, Long version, String caption, String status) {

	}
}
