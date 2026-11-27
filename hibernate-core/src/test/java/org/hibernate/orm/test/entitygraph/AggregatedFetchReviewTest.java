package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.FetchMethod;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { AggregatedFetchReviewTest.Owner.class,
		AggregatedFetchReviewTest.SecondaryChild.class, AggregatedFetchReviewTest.EmbeddedChild.class,
		AggregatedFetchReviewTest.Target.class })
@SessionFactory(useCollectingStatementInspector = true)
class AggregatedFetchReviewTest {
	@AfterEach void clean(SessionFactoryScope scope) { scope.getSessionFactory().getSchemaManager().truncate(); }

	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void secondaryTableSameColumnNames(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			Owner owner = new Owner(); owner.id = 1L; s.persist(owner);
			SecondaryChild child = new SecondaryChild(); child.id = 1L; child.owner = owner;
			child.primaryLabel = "main"; child.secondaryLabel = "secondary"; s.persist(child);
		});
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class);
			graph.addAttributeNode("secondaryChildren").addOption(method);
			var owner = s.find(graph, 1L);
			assertThat(owner.secondaryChildren).hasSize(1);
			assertThat(owner.secondaryChildren.get(0).primaryLabel).isEqualTo("main");
			assertThat(owner.secondaryChildren.get(0).secondaryLabel).isEqualTo("secondary");
		});
	}

	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void restrictedToOneInsideEmbeddedChild(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			Owner owner = new Owner(); owner.id = 1L; s.persist(owner);
			Target target = new Target(); target.id = 10L; target.active = false; s.persist(target);
			EmbeddedChild child = new EmbeddedChild(); child.id = 1L; child.owner = owner;
			child.relation = new Relation(); child.relation.target = target; child.relation.text = "keep";
			s.persist(child);
		});
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class);
			graph.addAttributeNode("embeddedChildren").addOption(method);
			var owner = s.find(graph, 1L);
			assertThat(owner.embeddedChildren).hasSize(1);
			assertThat(owner.embeddedChildren.get(0).relation.target == null).isTrue();
		});
	}

	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void restrictedToOneInsideElementCollection(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			Target target = new Target(); target.id = 10L; target.active = false; s.persist(target);
			Owner owner = new Owner(); owner.id = 1L;
			Relation relation = new Relation(); relation.target = target; relation.text = "keep";
			owner.relations.add(relation); s.persist(owner);
		});
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class);
			graph.addAttributeNode("relations").addOption(method);
			var owner = s.find(graph, 1L);
			assertThat(owner.relations).hasSize(1);
			assertThat(owner.relations.get(0).target).isNull();
		});
	}

	@Entity(name = "AROwner") static class Owner {
		@Id Long id;
		@OneToMany(mappedBy = "owner") List<SecondaryChild> secondaryChildren = new ArrayList<>();
		@OneToMany(mappedBy = "owner") List<EmbeddedChild> embeddedChildren = new ArrayList<>();
		@ElementCollection List<Relation> relations = new ArrayList<>();
	}
	@Entity(name = "ARSecondaryChild") @SecondaryTable(name = "ARChildExtra") static class SecondaryChild {
		@Id Long id;
		@ManyToOne(fetch = FetchType.LAZY) Owner owner;
		@Column(name = "label") String primaryLabel;
		@Column(name = "label", table = "ARChildExtra") String secondaryLabel;
	}
	@Entity(name = "AREmbeddedChild") static class EmbeddedChild {
		@Id Long id;
		@ManyToOne(fetch = FetchType.LAZY) Owner owner;
		@Embedded Relation relation;
	}
	@Embeddable static class Relation {
		String text;
		@ManyToOne(fetch = FetchType.LAZY) Target target;
	}
	@Entity(name = "ARTarget") @SQLRestriction("active = true") static class Target {
		@Id Long id;
		boolean active;
	}
}
