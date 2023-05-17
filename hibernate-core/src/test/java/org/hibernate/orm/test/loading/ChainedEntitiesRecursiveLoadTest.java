package org.hibernate.orm.test.loading;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author gtoison
 *
 */
@SessionFactory
@DomainModel(annotatedClasses = {ChainedEntitiesRecursiveLoadTest.Container.class})
public class ChainedEntitiesRecursiveLoadTest {


	@Test public void test(SessionFactoryScope scope) {
		// Create a bunch of chained entities parent/child association and get the last child
		Container container = scope.fromSession(s -> {
			s.getSession().beginTransaction();
			
			Container c = null;
			// Assuming that each recursive load needs a few method calls 1000 entities should be enough to trigger a stack overflow
			for (int i=0; i<1000; i++) {
				Container child = new Container();
				child.parent = c;
				
				c = child;
				
				s.persist(c);
			}
			
			s.getSession().getTransaction().commit();
			
			return c;
		});
		
		// Attempt to load the last child
		scope.inSession(s -> s.getReference(Container.class, container.id).toString());
	}

	@Entity
	@Proxy(lazy = false)
	public static class Container {

		@Id @GeneratedValue Long id;

		@Fetch(FetchMode.SELECT)
		@ManyToOne
		Container parent;
	}
}
