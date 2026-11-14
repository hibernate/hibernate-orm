package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.FetchMethod;
import org.hibernate.LockMode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = { AggregatedLockReviewTest.Owner.class, AggregatedLockReviewTest.Child.class })
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialect(H2Dialect.class)
class AggregatedLockReviewTest {
	@AfterEach void clean(SessionFactoryScope scope) { scope.getSessionFactory().getSchemaManager().truncate(); }
	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void explicitChildLockMustExcludeConcurrentUpdate(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			Owner owner = new Owner(); owner.id = 1L; s.persist(owner);
			Child child = new Child(); child.id = 1L; child.owner = owner; child.text = "before"; s.persist(child);
		});
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class); graph.addAttributeNode("children").addOption(method);
			var owner = s.find(graph, 1L, LockModeType.PESSIMISTIC_WRITE);
			var child = owner.children.get(0);
			System.out.println("Child mode before explicit lock: " + method + " " + s.getCurrentLockMode(child));
			scope.getCollectingStatementInspector().clear();
			s.lock(child, LockMode.PESSIMISTIC_WRITE);
			System.out.println("Explicit child lock SQL: " + scope.getCollectingStatementInspector().getSqlQueries());
			assertThatThrownBy(() -> scope.inTransaction(other -> {
				other.createNativeMutationQuery("set lock_timeout 100").executeUpdate();
				other.createMutationQuery("update ALChild set text = 'concurrent' where id = 1").executeUpdate();
			})).isInstanceOfAny(
					jakarta.persistence.PessimisticLockException.class,
					jakarta.persistence.LockTimeoutException.class,
					org.hibernate.PessimisticLockException.class
			);
		});
	}
	@Entity(name = "ALOwner") static class Owner {
		@Id Long id;
		@OneToMany(mappedBy = "owner") List<Child> children = new ArrayList<>();
	}
	@Entity(name = "ALChild") static class Child {
		@Id Long id;
		@ManyToOne(fetch = FetchType.LAZY) Owner owner;
		String text;
	}
}
