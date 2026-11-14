package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.FetchMethod;
import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = AggregatedCacheReviewTest.Owner.class)
@SessionFactory(useCollectingStatementInspector = true, generateStatistics = true)
@ServiceRegistry(settings = {
	@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
	@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
})
class AggregatedCacheReviewTest {
	@AfterEach void clean(SessionFactoryScope scope) { scope.getSessionFactory().getSchemaManager().truncate(); }
	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void cacheWithPreviouslyUninitializedCollection(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			Owner owner = new Owner(); owner.id = 1L; owner.tags.add("one"); owner.tags.add("two"); s.persist(owner);
		});
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.inTransaction(s -> {
			var existing = s.find(Owner.class, 1L);
			assertThat(Hibernate.isInitialized(existing.tags)).isFalse();
			var graph = s.createEntityGraph(Owner.class); graph.addAttributeNode("tags").addOption(method);
			var owner = s.createSelectionQuery("from ACOwner", graph).setCacheable(true).getSingleResult();
			assertThat(owner.tags).hasSize(2);
		});
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class); graph.addAttributeNode("tags").addOption(method);
			var owner = s.createSelectionQuery("from ACOwner", graph).setCacheable(true).getSingleResult();
			assertThat(owner.tags).hasSize(2);
		});
	}
	@Entity(name = "ACOwner") static class Owner {
		@Id Long id;
		@ElementCollection List<String> tags = new ArrayList<>();
	}
}
