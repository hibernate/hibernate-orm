package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.OFFSET;

@DomainModel(annotatedClasses = {
		CollectionFetchPredicatePaginationTest.RootNode.class,
		CollectionFetchPredicatePaginationTest.ParentNode.class,
		CollectionFetchPredicatePaginationTest.ChildNode.class,
		CollectionFetchPredicatePaginationTest.FlagNode.class
})
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-20846")
public class CollectionFetchPredicatePaginationTest {
	private static final String QUERY = "select r from RootNode r "
			+ "join fetch r.parent p join fetch p.children c "
			+ "where r.childKey = cast(c.id as string) order by r.id";

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 4; id++ ) {
				final ParentNode parent = new ParentNode();
				parent.id = id;
				for ( long childId = id * 10; childId < id * 10 + 2; childId++ ) {
					final ChildNode child = new ChildNode();
					child.id = childId;
					child.parent = parent;
					parent.children.add( child );
				}
				session.persist( parent );
				final RootNode root = new RootNode();
				root.id = id;
				root.childKey = Long.toString( id * 10 );
				root.parent = parent;
				session.persist( root );
				final FlagNode flag = new FlagNode();
				flag.id = id;
				flag.parent = parent;
				flag.enabled = id % 2 == 0;
				session.persist( flag );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void fetchedChildPredicateWithoutPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<RootNode> roots = session.createSelectionQuery( QUERY, RootNode.class ).list();
			assertEquals( List.of( 1L, 2L, 3L, 4L ), roots.stream().map( root -> root.id ).toList() );
			for ( RootNode root : roots ) {
				assertEquals( 1, root.parent.children.size() );
				assertEquals( root.childKey, Long.toString( root.parent.children.get( 0 ).id ) );
			}
		} );
	}

	@Test
	void fetchedChildPredicateWithPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<RootNode> roots = session.createSelectionQuery( QUERY, RootNode.class )
					.setFirstResult( 1 ).setMaxResults( 2 ).list();
			assertEquals( List.of( 2L, 3L ), roots.stream().map( root -> root.id ).toList() );
			for ( RootNode root : roots ) {
				assertEquals( 1, root.parent.children.size() );
				assertEquals( root.childKey, Long.toString( root.parent.children.get( 0 ).id ) );
			}
		} );
	}

	@Test
	void fetchedChildPredicateWithZeroOffset(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<RootNode> roots = session.createSelectionQuery( QUERY, RootNode.class )
					.setFirstResult( 0 ).setMaxResults( 2 ).list();
			assertEquals( List.of( 1L, 2L ), roots.stream().map( root -> root.id ).toList() );
		} );
	}

	@Test
	void independentJoinPredicateWithPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<RootNode> roots = session.createSelectionQuery(
					"select r from RootNode r join fetch r.parent p left join fetch p.children "
							+ "join FlagNode f on f.parent = p where f.enabled = true order by r.id",
					RootNode.class
			).setFirstResult( 0 ).setMaxResults( 1 ).list();
			assertEquals( List.of( 2L ), roots.stream().map( root -> root.id ).toList() );
			assertEquals( 2, roots.get( 0 ).parent.children.size() );
		} );
	}

	@Test
	void independentJoinBeforeCollectionFetchWithPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<RootNode> roots = session.createSelectionQuery(
					"select r from RootNode r join fetch r.parent p join FlagNode f on f.parent = p "
							+ "left join fetch p.children where f.enabled = true order by r.id",
					RootNode.class
			).setFirstResult( 0 ).setMaxResults( 1 ).list();
			assertEquals( List.of( 2L ), roots.stream().map( root -> root.id ).toList() );
			assertEquals( 2, roots.get( 0 ).parent.children.size() );
		} );
	}

	@Test
	void independentSubqueryPredicateWithPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var sql = scope.getCollectingStatementObserver();
			sql.clear();
			final List<RootNode> roots = session.createSelectionQuery(
					"select r from RootNode r join fetch r.parent p left join fetch p.children "
							+ "where exists (select f.id from FlagNode f where f.parent = p and f.enabled = true) "
							+ "order by r.id",
					RootNode.class
			).setFirstResult( 0 ).setMaxResults( 1 ).list();
			assertEquals( List.of( 2L ), roots.stream().map( root -> root.id ).toList() );
			assertEquals( 2, roots.get( 0 ).parent.children.size() );
			assertEquals( scope.getSessionFactory().getJdbcServices().getDialect()
					.getSubquerySupport().supports( OFFSET ),
					sql.getSqlQueries().get( 0 ).toLowerCase().contains( "from (select" ) );
		} );
	}

	@Entity(name = "FlagNode")
	public static class FlagNode {
		@Id
		long id;
		@ManyToOne
		ParentNode parent;
		boolean enabled;
	}

	@Entity(name = "RootNode")
	public static class RootNode {
		@Id
		long id;
		String childKey;
		@ManyToOne
		ParentNode parent;
	}

	@Entity(name = "ParentNode")
	public static class ParentNode {
		@Id
		long id;
		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		List<ChildNode> children = new ArrayList<>();
	}

	@Entity(name = "ChildNode")
	public static class ChildNode {
		@Id
		long id;
		@ManyToOne
		ParentNode parent;
	}
}
