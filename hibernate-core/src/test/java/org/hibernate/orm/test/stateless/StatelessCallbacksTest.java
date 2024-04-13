package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = StatelessCallbacksTest.WithCallbacks.class)
public class StatelessCallbacksTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inStatelessSession(s ->  {
			WithCallbacks instance = new WithCallbacks();
			instance.name = "gavin";
			s.insert(instance);
			// because the semantics of @PrePersist and @PreRemove
			// are inappropriate for a StatelessSession, the @Pre
			// don't get called. However, the @Post events do make
			// sense, since they correspond to database operations
			assertFalse(instance.prePersist);
			assertTrue(instance.postPersist);
			s.update(instance);
			assertFalse(instance.preUpdate);
			assertTrue(instance.postUpdate);
			s.delete(instance);
			assertFalse(instance.preRemove);
			assertTrue(instance.postRemove);
		});
	}

	@Entity
	static class WithCallbacks {
		boolean prePersist = false;
		boolean preUpdate = false;
		boolean preRemove = false;
		boolean postPersist = false;
		boolean postUpdate = false;
		boolean postRemove = false;

		@GeneratedValue @Id
		Long id;
		String name;
		@PrePersist
		void prePersist() {
			prePersist = true;
		}
		@PostPersist
		void postPersist() {
			postPersist = true;
		}
		@PreUpdate
		void preUpdate() {
			preUpdate = true;
		}
		@PostUpdate
		void postUpdate() {
			postUpdate = true;
		}
		@PreRemove
		void preRemove() {
			preRemove = true;
		}
		@PostRemove
		void postRemove() {
			postRemove = true;
		}
	}
}
