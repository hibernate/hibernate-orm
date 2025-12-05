/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;

import jakarta.persistence.PersistenceException;
import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for <code>Immutable</code> annotation.
 *
 * @author Hardy Ferentschik
 */
@DomainModel(
		annotatedClasses = {
				Country.class,
				State.class,
				Photo.class
		}
)
@SessionFactory
public class ImmutableTest {

	@AfterEach
	public void afterEach(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testImmutableEntity(SessionFactoryScope scope) {
		Country c = new Country();
		scope.inTransaction(
				session -> {
					c.setName( "Germany" );
					session.persist( c );
				}
		);

		// try changing the entity
		scope.inTransaction(
				session -> {
					Country germany = session.find( Country.class, c.getId() );
					assertThat( germany ).isNotNull();
					germany.setName( "France" );
					assertThat( germany.getName() )
							.describedAs( "Local name can be changed" )
							.isEqualTo( "France" );

					session.persist( germany );
				}
		);

		// retrieving the country again - it should be unmodified
		scope.inTransaction(
				session -> {
					Country germany = session.find( Country.class, c.getId() );
					assertThat( germany ).isNotNull();
					assertThat( germany.getName() )
							.describedAs( "Name should not have changed" )
							.isEqualTo( "Germany" );

				}
		);
	}

	@Test
	public void testImmutableCollection(SessionFactoryScope scope) {
		Country country = new Country();
		country.setName( "Germany" );
		List<State> states = new ArrayList<>();
		State bayern = new State();
		bayern.setName( "Bayern" );
		State hessen = new State();
		hessen.setName( "Hessen" );
		State sachsen = new State();
		sachsen.setName( "Sachsen" );
		states.add( bayern );
		states.add( hessen );
		states.add( sachsen );
		country.setStates( states );

		scope.inTransaction(
				session -> session.persist( country )
		);

		PersistenceException persistenceException = assertThrows( PersistenceException.class, () -> scope.inTransaction(
				session -> {
					Country germany = session.find( Country.class, country.getId() );
					assertThat( germany ).isNotNull();
					assertThat( germany.getStates().size() )
							.describedAs( "Wrong number of states" )
							.isEqualTo( 3 );

					// try adding a state
					State foobar = new State();
					foobar.setName( "foobar" );
					session.persist( foobar );
					germany.getStates().add( foobar );
				}
		) );

		assertThat( persistenceException.getMessage() ).contains( "Immutable collection was modified" );

		persistenceException = assertThrows( PersistenceException.class, () -> scope.inTransaction(
				session -> {
					Country germany = session.find( Country.class, country.getId() );
					assertThat( germany ).isNotNull();
					assertThat( germany.getStates().size() )
							.describedAs( "Wrong number of states" )
							.isEqualTo( 3 );
					// try deleting a state
					germany.getStates().remove( 0 );
				}
		) );

		assertThat( persistenceException.getMessage() ).contains( "Immutable collection was modified" );

		scope.inTransaction(
				session -> {
					Country germany = session.find( Country.class, country.getId() );
					assertThat( germany ).isNotNull();
					assertThat( germany.getStates().size() )
							.describedAs( "Wrong number of states" )
							.isEqualTo( 3 );
				}
		);
	}


	@Test
	public void testMisplacedImmutableAnnotation() {
		MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
				.addAnnotatedClass( Foobar.class );
		try {
			metadataSources.buildMetadata();
//			fail( "Expecting exception due to misplaced @Immutable annotation");
		}
		catch (AnnotationException ignore) {
			fail( "Exception with @Immutable on field" );
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if ( metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

}
