/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections.semantics;

import java.util.ArrayList;

import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = TheEntityWithUniqueList.class )
@SessionFactory
public class CustomSemanticsTest {

	@Test
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( TheEntityWithUniqueList.class, (entityDescriptor) -> {
			final Property strings = entityDescriptor.getProperty( "strings" );
			final org.hibernate.mapping.Collection collectionDescriptor = (org.hibernate.mapping.Collection) strings.getValue();
			final org.hibernate.collection.spi.CollectionSemantics collectionSemantics = collectionDescriptor.getCollectionSemantics();
			assertThat( collectionSemantics ).isInstanceOf( UniqueListSemantic.class );
		} );
	}

	@Test
	public void testBasicUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final TheEntityWithUniqueList entity = new TheEntityWithUniqueList( 1, "first" );
			entity.setStrings( new ArrayList<>() );
			entity.getStrings().add( "the string" );
			entity.getStrings().add( "another" );
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final TheEntityWithUniqueList loaded = session.createQuery( "from TheEntityWithUniqueList", TheEntityWithUniqueList.class ).uniqueResult();
			// try to re-add one, should throw IllegalArgumentException
			try {
				loaded.getStrings().add( "another" );
				fail( "Expecting IllegalArgumentException" );
			}
			catch (IllegalArgumentException expected) {
				// expected outcome
			}
		} );
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete TheEntityWithUniqueList" ).executeUpdate();
		} );
	}

}
