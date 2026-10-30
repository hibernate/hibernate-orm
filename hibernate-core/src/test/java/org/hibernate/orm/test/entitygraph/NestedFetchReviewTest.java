package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import org.hibernate.FetchMethod;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.RootGraph;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { NestedFetchReviewTest.Parent.class,
		NestedFetchReviewTest.Child.class, NestedFetchReviewTest.Target.class })
@SessionFactory(useCollectingStatementInspector = true, generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
})
class NestedFetchReviewTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Parent parent = new Parent();
			parent.id = 1L;
			session.persist(parent);
			for (long i = 1; i <= 2; i++) {
				Target target = new Target();
				target.id = i;
				target.name = "t" + i;
				session.persist(target);
				Child child = new Child();
				child.id = i;
				child.name = "c" + i;
				child.parent = parent;
				child.target = target;
				child.tags.add("tag" + i);
				parent.children.add(child);
				session.persist(child);
			}
		});
		scope.getCollectingStatementInspector().clear();
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();
	}

	@AfterEach
	void clean(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private RootGraph<Parent> graph(org.hibernate.Session session) {
		return graph(session, FetchMethod.NESTED);
	}

	private RootGraph<Parent> graph(org.hibernate.Session session, FetchMethod method) {
		var graph = session.createEntityGraph(Parent.class);
		graph.addAttributeNode("children").addOption(method);
		return graph;
	}

	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void cacheAfterCollectionAlreadyInitialized(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var parent = session.find(Parent.class, 1L);
			Hibernate.initialize(parent.children);
			var result = session.createSelectionQuery("from ReviewNestedParent", graph(session, method))
					.setCacheable(true).getSingleResult();
			assertThat(result.children).hasSize(2);
		});
		scope.inTransaction(session -> {
			var result = session.createSelectionQuery("from ReviewNestedParent", graph(session, method))
					.setCacheable(true).getSingleResult();
			assertThat(scope.getSessionFactory().getStatistics().getQueryCacheHitCount()).isEqualTo(1);
			assertThat(result.children).hasSize(2);
		});
	}

	@Test
	void cacheFromFreshSession(SessionFactoryScope scope) {
		for (int i = 0; i < 2; i++) {
			scope.inTransaction(session -> {
				var result = session.createSelectionQuery("from ReviewNestedParent", graph(session))
						.setCacheable(true).getSingleResult();
				assertThat(result.children).hasSize(2);
			});
		}
		assertThat(scope.getSessionFactory().getStatistics().getQueryCacheHitCount()).isEqualTo(1);
	}

	@Test
	void explicitJoinWithRestriction(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var result = session.createSelectionQuery(
					"select p from ReviewNestedParent p join p.children c where c.name = 'c1'", graph(session))
					.getSingleResult();
			assertThat(result.children).extracting(c -> c.name).containsExactly("c1", "c2");
		});
	}

	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void implicitJoinWithRestriction(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var result = session.createSelectionQuery(
					"from ReviewNestedParent p where element(p.children).name = 'c1'", graph(session, method))
					.getSingleResult();
			assertThat(result.children).extracting(c -> c.name).containsExactly("c1", "c2");
		});
	}

	@Test
	void batchedToOneSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var graph = graph(session);
			graph.addSubgraph("children").addAttributeNode("target");
			var result = session.find(graph, 1L);
			assertThat(result.children).hasSize(2);
			for (var child : result.children) {
				assertThat(Hibernate.isInitialized(child.target)).isTrue();
				assertThat(Hibernate.unproxy(child.target, Target.class).name).isEqualTo("t" + child.id);
			}
		});
	}

	@Test
	void collectionInElementSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var graph = graph(session);
			graph.addSubgraph("children").addAttributeNode("tags").addOption(FetchMethod.NESTED);
			var result = session.find(graph, 1L);
			assertThat(result.children).hasSize(2);
			for (var child : result.children) {
				assertThat(Hibernate.isInitialized(child.tags)).isTrue();
				assertThat(child.tags).containsExactly("tag" + child.id);
			}
		});
	}

	@Entity(name = "ReviewNestedParent")
	static class Parent {
		@Id Long id;
		@OneToMany(mappedBy = "parent")
		@OrderBy("id")
		List<Child> children = new ArrayList<>();
	}

	@Entity(name = "ReviewNestedChild")
	static class Child {
		@Id Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY) Parent parent;
		@ManyToOne(fetch = FetchType.LAZY) Target target;
		@ElementCollection List<String> tags = new ArrayList<>();
	}

	@Entity(name = "ReviewNestedTarget")
	@BatchSize(size = 10)
	static class Target {
		@Id Long id;
		String name;
	}
}
