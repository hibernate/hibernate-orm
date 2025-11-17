/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.ordered;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.SortDirection;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragmentImpl;
import org.hibernate.metamodel.mapping.ordering.ast.OrderingSpecification;
import org.hibernate.persister.collection.BasicCollectionPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 * @author Lukasz Antoniak
 */
@DomainModel(
		annotatedClasses = {
				Person.class
		}
)
@SessionFactory
public class ElementCollectionSortingTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSortingElementCollectionSyntax(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Person p join fetch p.nickNamesAscendingNaturalSort" ).list();
					session.createQuery( "from Person p join fetch p.nickNamesDescendingNaturalSort" ).list();
					session.createQuery( "from Person p join fetch p.addressesAscendingNaturalSort" ).list();
					session.createQuery( "from Person p join fetch p.addressesDescendingNaturalSort" ).list();
					session.createQuery( "from Person p join fetch p.addressesCityAscendingSort" ).list();
					session.createQuery( "from Person p join fetch p.addressesCityDescendingSort" ).list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-6875")
	public void testSortingEmbeddableCollectionOfPrimitives(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Person steve = new Person();
					steve.setName( "Steve" );
					steve.getNickNamesAscendingNaturalSort().add( "sebersole" );
					steve.getNickNamesAscendingNaturalSort().add( "ebersole" );
					steve.getNickNamesDescendingNaturalSort().add( "ebersole" );
					steve.getNickNamesDescendingNaturalSort().add( "sebersole" );

					final Person lukasz = new Person();
					lukasz.setName( "Lukasz" );
					lukasz.getNickNamesAscendingNaturalSort().add( "antoniak" );
					lukasz.getNickNamesAscendingNaturalSort().add( "lantoniak" );
					lukasz.getNickNamesDescendingNaturalSort().add( "lantoniak" );
					lukasz.getNickNamesDescendingNaturalSort().add( "antoniak" );

					session.persist( steve );
					session.persist( lukasz );
					session.flush();

					session.clear();

					final List<String> lukaszNamesAsc = Arrays.asList( "antoniak", "lantoniak" );
					final List<String> lukaszNamesDesc = Arrays.asList( "lantoniak", "antoniak" );
					final List<String> steveNamesAsc = Arrays.asList( "ebersole", "sebersole" );
					final List<String> steveNamesDesc = Arrays.asList( "sebersole", "ebersole" );

					// Testing object graph navigation. Lazy loading collections.
					checkPersonNickNames(
							lukaszNamesAsc,
							lukaszNamesDesc,
							(Person) session.get( Person.class, lukasz.getId() )
					);
					checkPersonNickNames(
							steveNamesAsc,
							steveNamesDesc,
							(Person) session.get( Person.class, steve.getId() )
					);

					session.clear();

					// Testing HQL query. Eagerly fetching nicknames.
					final List<Person> result = session.createQuery(
							"select distinct p from Person p join fetch p.nickNamesAscendingNaturalSort join fetch p.nickNamesDescendingNaturalSort order by p.name"
					).list();
					assertEquals( 2, result.size() );
					checkPersonNickNames( lukaszNamesAsc, lukaszNamesDesc, result.get( 0 ) );
					checkPersonNickNames( steveNamesAsc, steveNamesDesc, result.get( 1 ) );

					// Metadata verification.
					checkSQLOrderBy(
							session,
							Person.class.getName(),
							"nickNamesAscendingNaturalSort",
							SortDirection.ASCENDING
					);
					checkSQLOrderBy(
							session,
							Person.class.getName(),
							"nickNamesDescendingNaturalSort",
							SortDirection.DESCENDING
					);
				}
		);
	}

	private void checkSQLOrderBy(Session session, String entityName, String propertyName, SortDirection order) {
		String roleName = entityName + "." + propertyName;
		BasicCollectionPersister collectionPersister = (BasicCollectionPersister) session
				.unwrap( SessionImplementor.class )
				.getFactory()
				.getMappingMetamodel()
				.getCollectionDescriptor( roleName );
		assertTrue( collectionPersister.hasOrdering() );
		PluralAttributeMapping attributeMapping = collectionPersister.getAttributeMapping();
		assertThat( attributeMapping.getFetchableName(), is( propertyName ) );
		OrderByFragmentImpl orderByFragment = (OrderByFragmentImpl) attributeMapping.getOrderByFragment();
		List<OrderingSpecification> fragmentSpecs = orderByFragment.getFragmentSpecs();
		assertThat( fragmentSpecs.size(), is( 1 ) );
		assertThat( fragmentSpecs.get( 0 ).getSortOrder(), is( order ) );
	}

	private void checkPersonNickNames(List<String> expectedAscending, List<String> expectedDescending, Person person) {
		// Comparing lists to verify ordering.
		assertEquals( expectedAscending, new ArrayList<String>( person.getNickNamesAscendingNaturalSort() ) );
		assertEquals( expectedDescending, new ArrayList<String>( person.getNickNamesDescendingNaturalSort() ) );
	}
}
