package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pagination over a collection fetched through {@code treat()} on the query root.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationTreatTest.BaseEntity.class,
		CollectionFetchPaginationTreatTest.SubEntity.class,
		CollectionFetchPaginationTreatTest.ChildEntity.class
})
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
@JiraKey("HHH-21145")
public class CollectionFetchPaginationTreatTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final SubEntity a = new SubEntity( 1L, "A" );
			a.addChild( new ChildEntity( 10L, 0 ) );
			a.addChild( new ChildEntity( 11L, 1 ) );
			s.persist( a );
			final SubEntity b = new SubEntity( 2L, "B" );
			b.addChild( new ChildEntity( 20L, 0 ) );
			s.persist( b );
			s.persist( new SubEntity( 3L, "C" ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void everyPageReturnsItsRoot(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			assertEquals( List.of( "A" ), page( s, 0 ) );
			assertEquals( List.of( "B" ), page( s, 1 ) );
			assertEquals( List.of( "C" ), page( s, 2 ) );
		} );
	}

	@Test
	void fetchedCollectionIsComplete(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final List<BaseEntity> result = query( s ).setFirstResult( 0 ).setMaxResults( 2 ).getResultList();
			assertEquals( 2, result.size() );
			final SubEntity a = (SubEntity) result.get( 0 );
			assertTrue( Hibernate.isInitialized( a.children ) );
			assertEquals( 2, a.children.size() );
		} );
	}

	private static List<String> page(org.hibernate.Session s, int page) {
		return query( s ).setFirstResult( page ).setMaxResults( 1 ).getResultList()
				.stream().map( e -> e.code ).toList();
	}

	private static jakarta.persistence.TypedQuery<BaseEntity> query(org.hibernate.Session s) {
		final CriteriaBuilder cb = s.getCriteriaBuilder();
		final CriteriaQuery<BaseEntity> q = cb.createQuery( BaseEntity.class );
		final Root<BaseEntity> root = q.from( BaseEntity.class );
		cb.treat( root, SubEntity.class ).fetch( "children", JoinType.LEFT );
		q.select( root ).distinct( true ).orderBy( cb.asc( root.get( "code" ) ) );
		return s.createQuery( q );
	}

	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseEntity {
		@Id
		Long id;
		String code;

		BaseEntity() {
		}

		BaseEntity(Long id, String code) {
			this.id = id;
			this.code = code;
		}
	}

	@Entity(name = "SubEntity")
	public static class SubEntity extends BaseEntity {
		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		List<ChildEntity> children = new ArrayList<>();

		SubEntity() {
		}

		SubEntity(Long id, String code) {
			super( id, code );
		}

		void addChild(ChildEntity child) {
			child.parent = this;
			children.add( child );
		}
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity {
		@Id
		Long id;
		@ManyToOne
		SubEntity parent;
		int sortOrder;

		ChildEntity() {
		}

		ChildEntity(Long id, int sortOrder) {
			this.id = id;
			this.sortOrder = sortOrder;
		}
	}
}
