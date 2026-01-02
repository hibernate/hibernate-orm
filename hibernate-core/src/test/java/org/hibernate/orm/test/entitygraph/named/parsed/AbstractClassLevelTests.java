/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed;

import org.hibernate.DuplicateMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.internal.InvalidNamedEntityGraphParameterException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.BadRootClassEntity;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.Book;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.DomesticPublishingHouse;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.Duplicator;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.ForeignPublishingHouse;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.Isbn;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.Person;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.Publisher;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.PublishingHouse;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.entitygraph.parser.AssertionHelper.assertBasicAttributes;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for Hibernate's {@link org.hibernate.annotations.NamedEntityGraph @NamedEntityGraph}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public abstract class AbstractClassLevelTests {

	@Test
	@DomainModel(annotatedClasses = {
			Book.class,
			Person.class,
			Publisher.class,
			PublishingHouse.class,
			DomesticPublishingHouse.class,
			ForeignPublishingHouse.class,
			Isbn.class

	})
	@SessionFactory(exportSchema = false)
	void testRegistrations(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		assertBasicAttributes( sessionFactory.findEntityGraphByName( "book-title-isbn" ), "title", "isbn" );

		assertBasicAttributes(
				sessionFactory.findEntityGraphByName( "book-title-isbn-author" ),
				"title",
				"isbn",
				"author"
		);

		assertBasicAttributes(
				sessionFactory.findEntityGraphByName( "book-title-isbn-editor" ),
				"title",
				"isbn",
				"editor"
		);

		assertBasicAttributes(
				sessionFactory.findEntityGraphByName( "publishing-house-bio" ),
				"name",
				"ceo",
				"boardMembers"
		);
	}

	@Test
	@DomainModel(annotatedClasses = BadRootClassEntity.class)
	void testRootEntityDifferentFromEntityMarkedWithAnnotation(DomainModelScope modelScope) {
		final MetadataImplementor domainModel = modelScope.getDomainModel();

		try (org.hibernate.SessionFactory sessionFactory = domainModel.buildSessionFactory()) {
			fail( "Expecting an exception" );
		}
		catch (InvalidNamedEntityGraphParameterException expected) {
		}
	}

	@Test
	@ServiceRegistry
	void testDuplicateNames(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Duplicator.class );
		try {
			metadataSources.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (DuplicateMappingException expected) {
		}
	}

}
