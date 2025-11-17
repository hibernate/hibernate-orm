/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed;

import org.hibernate.DuplicateMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.orm.test.entitygraph.named.parsed.pkg.Book;
import org.hibernate.orm.test.entitygraph.named.parsed.pkg.Duplicator;
import org.hibernate.orm.test.entitygraph.named.parsed.pkg.Isbn;
import org.hibernate.orm.test.entitygraph.named.parsed.pkg.Person;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.entitygraph.parser.AssertionHelper.assertBasicAttributes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class PackageLevelTests {
	@Test
	@DomainModel(
			annotatedClasses = { Book.class, Isbn.class, Person.class },
			annotatedPackageNames = "org.hibernate.orm.test.entitygraph.named.parsed.pkg"
	)
	@SessionFactory(exportSchema = false)
	void testDiscovery(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		assertBasicGraph( sessionFactory, "book-title-isbn", "title", "isbn" );
		assertBasicGraph( sessionFactory, "book-title-isbn-author", "title", "isbn", "author" );
		assertBasicGraph( sessionFactory, "book-title-isbn-editor", "title", "isbn", "editor" );
	}

	private static void assertBasicGraph(SessionFactoryImplementor sessionFactory, String name, String... names) {
		RootGraphImplementor<?> graph = sessionFactory.findEntityGraphByName( name );
		assertEquals( name, graph.getName() );
		assertBasicAttributes( graph, names );
	}

	@Test
	@ServiceRegistry
	void testDuplication(ServiceRegistryScope registryScope) {
		final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();
		try {
			new MetadataSources( serviceRegistry )
					.addAnnotatedClass( Duplicator.class )
					.addPackage( "org.hibernate.orm.test.entitygraph.named.parsed.pkg" )
					.buildMetadata();
			fail( "Expected an exception" );
		}
		catch (DuplicateMappingException expected) {
		}
	}

	@Test
	@DomainModel(
			annotatedClasses = Person.class,
			annotatedPackageNames = "org.hibernate.orm.test.entitygraph.named.parsed.pkg2"
	)
	void testInvalid(DomainModelScope modelScope) {
		try (org.hibernate.SessionFactory sessionFactory = modelScope.getDomainModel().buildSessionFactory()) {
			fail( "Expected an exception" );
		}
		catch (InvalidGraphException expected) {
		}
	}
}
