package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.FetchMethod;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { AggregatedSubselectReviewTest.Owner.class, AggregatedSubselectReviewTest.Child.class })
@SessionFactory(useCollectingStatementInspector = true)
class AggregatedSubselectReviewTest {
	@AfterEach void clean(SessionFactoryScope scope) { scope.getSessionFactory().getSchemaManager().truncate(); }
	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void subselectCollectionOfNestedElement(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			Owner owner = new Owner(); owner.id = 1L; s.persist(owner);
			for (long i = 1; i <= 2; i++) {
				Child child = new Child(); child.id = i; child.owner = owner;
				child.tags.add("tag" + i); s.persist(child);
			}
		});
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class); graph.addAttributeNode("children").addOption(method);
			graph.addSubgraph("children").addAttributeNode("tags").addOption(FetchMethod.BY_SUBQUERY);
			var owner = s.createSelectionQuery("from ASOwner", graph).getSingleResult();
			assertThat(owner.children).hasSize(2);
			for (var child : owner.children) { assertThat(child.tags).containsExactly("tag" + child.id); }
		});
	}
	@Entity(name = "ASOwner") static class Owner {
		@Id Long id;
		@OneToMany(mappedBy = "owner") List<Child> children = new ArrayList<>();
	}
	@Entity(name = "ASChild") static class Child {
		@Id Long id;
		@ManyToOne(fetch = FetchType.LAZY) Owner owner;
		@ElementCollection @Fetch(FetchMode.SUBSELECT) List<String> tags = new ArrayList<>();
	}
}
