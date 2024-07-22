/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.proxy.narrow;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Yoann Rodière
 * @author Guillaume Smet
 */
@DomainModel(
		annotatedClasses = {
				AbstractEntity.class, ConcreteEntity.class, LazyAbstractEntityReference.class
		}
)
@SessionFactory
public class ProxyNarrowingTest {

	@Test
	public void testNarrowedProxyIsInitializedIfOriginalProxyIsInitialized(SessionFactoryScope scope) {

		Integer entityReferenceId = scope.fromTransaction(
				session -> {
					ConcreteEntity entity = new ConcreteEntity();
					session.persist( entity );

					LazyAbstractEntityReference reference = new LazyAbstractEntityReference( entity );
					session.persist( reference );
					Integer id = reference.getId();

					session.flush();
					return id;
				}
		);


		scope.inTransaction(
				session -> {
					// load a proxified version of the entity into the session: the proxy is based on the AbstractEntity class
					// as the reference class property is of type AbstractEntity.
					LazyAbstractEntityReference reference = session.get(
							LazyAbstractEntityReference.class,
							entityReferenceId
					);
					AbstractEntity abstractEntityProxy = reference.getEntity();

					assertTrue( ( abstractEntityProxy instanceof HibernateProxy ) && !Hibernate.isInitialized(
							abstractEntityProxy ) );
					Hibernate.initialize( abstractEntityProxy );
					assertTrue( Hibernate.isInitialized( abstractEntityProxy ) );

					// load the concrete class via session.load to trigger the StatefulPersistenceContext.narrowProxy code
					ConcreteEntity concreteEntityProxy = session.load(
							ConcreteEntity.class,
							abstractEntityProxy.getId()
					);

					// the new proxy created should be initialized
					assertTrue( Hibernate.isInitialized( concreteEntityProxy ) );
					assertTrue( session.contains( concreteEntityProxy ) );

					// clean up
					session.remove( reference );
					session.remove( concreteEntityProxy );
				}
		);
	}

}
