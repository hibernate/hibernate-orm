/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed;

import org.hibernate.boot.model.internal.InvalidNamedEntityGraphParameterException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.entitygraph.named.parsed.pkg3.Book;
import org.hibernate.orm.test.entitygraph.named.parsed.pkg3.Person;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@ServiceRegistry(settings = @Setting(name = AvailableSettings.GRAPH_PARSER_MODE, value = "modern"))
public class PackageLevelTests extends AbstractPackageLevelTests {

	@Test
	@DomainModel(
			annotatedPackageNames = "org.hibernate.orm.test.entitygraph.named.parsed.pkg2"
	)
	void givenModernGraphParserModeShouldThrowExceptionWhenUsingAnnotationsWithoutRootAttribute(DomainModelScope modelScope) {
		try (org.hibernate.SessionFactory sessionFactory = modelScope.getDomainModel().buildSessionFactory()) {
			fail( "Expected an exception" );
		}
		catch (InvalidNamedEntityGraphParameterException expected) {
		}
	}

	@Test
	@DomainModel(
			annotatedClasses = { Book.class, Person.class },
			annotatedPackageNames = "org.hibernate.orm.test.entitygraph.named.parsed.pkg3"
	)
	@SessionFactory(exportSchema = false)
	void givenModernGraphParserModeShouldParseGraphCorrectlyWhenRootAttributeIsValid(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		assertBasicGraph( sessionFactory, "book-title-with-root-attribute", "title" );
		assertBasicGraph( sessionFactory, "book-title-author-editor-with-root-attribute", "title", "author", "editor" );
	}
}
