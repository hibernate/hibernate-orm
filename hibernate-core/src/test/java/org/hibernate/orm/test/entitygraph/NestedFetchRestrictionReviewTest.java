package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.FetchMethod;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {NestedFetchRestrictionReviewTest.Parent.class,
		NestedFetchRestrictionReviewTest.Child.class, NestedFetchRestrictionReviewTest.Target.class})
@SessionFactory(useCollectingStatementInspector = true)
class NestedFetchRestrictionReviewTest {
	@AfterEach
	void clean(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void restrictedToOne(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var parent = new Parent();
			parent.id = 1L;
			var child = new Child();
			child.id = 10L;
			child.name = "child";
			var target = new Target();
			target.id = 20L;
			target.name = "target";
			child.target = target;
			parent.children.add(child);
			session.persist(target);
			session.persist(child);
			session.persist(parent);
		});
		scope.inTransaction(session -> {
			var graph = session.createEntityGraph(Parent.class);
			graph.addAttributeNode("children").addOption(method);
			var parent = session.find(graph, 1L);
			assertThat(parent.children).hasSize(1);
			assertThat(parent.children.get(0).target == null).isTrue();
		});
	}

	@Entity(name = "ReviewRestrictedParent")
	static class Parent {
		@Id Long id;
		@OneToMany @JoinColumn(name = "parent_id") List<Child> children = new ArrayList<>();
	}
	@Entity(name = "ReviewRestrictedChild")
	static class Child {
		@Id Long id;
		String name;
		@ManyToOne(fetch = jakarta.persistence.FetchType.LAZY) Target target;
	}
	@Entity(name = "ReviewRestrictedTarget")
	@org.hibernate.annotations.SQLRestriction("visible = true")
	static class Target {
		boolean visible;
		@Id Long id;
		String name;
	}
}
