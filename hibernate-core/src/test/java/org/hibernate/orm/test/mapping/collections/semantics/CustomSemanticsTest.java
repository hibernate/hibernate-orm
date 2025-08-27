/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.semantics;

import java.util.ArrayList;

import org.hibernate.collection.internal.CustomCollectionTypeSemantics;
import org.hibernate.mapping.Property;
import org.hibernate.type.CustomCollectionType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ImplicitListAsListProvider;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsListProvider.class
		)
)
@DomainModel( annotatedClasses = { TheEntityWithUniqueList.class, TheEntityWithUniqueListRegistration.class } )
@SessionFactory
public class CustomSemanticsTest {

	@Test
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( TheEntityWithUniqueList.class, (entityDescriptor) -> {
			final Property strings = entityDescriptor.getProperty( "strings" );
			final org.hibernate.mapping.Collection collectionDescriptor = (org.hibernate.mapping.Collection) strings.getValue();
			assertThat( collectionDescriptor.getCollectionSemantics() ).isInstanceOf( CustomCollectionTypeSemantics.class );
			final CustomCollectionTypeSemantics semantics = (CustomCollectionTypeSemantics) collectionDescriptor.getCollectionSemantics();
			assertThat( semantics.getCollectionType() ).isInstanceOf( CustomCollectionType.class );
			final CustomCollectionType collectionType = (CustomCollectionType) semantics.getCollectionType();
			assertThat( collectionType.getUserType() ).isInstanceOf( UniqueListType.class );
		} );

		scope.withHierarchy( TheEntityWithUniqueListRegistration.class, (entityDescriptor) -> {
			final Property strings = entityDescriptor.getProperty( "strings" );
			final org.hibernate.mapping.Collection collectionDescriptor = (org.hibernate.mapping.Collection) strings.getValue();
			assertThat( collectionDescriptor.getCollectionSemantics() ).isInstanceOf( CustomCollectionTypeSemantics.class );
			final CustomCollectionTypeSemantics semantics = (CustomCollectionTypeSemantics) collectionDescriptor.getCollectionSemantics();
			assertThat( semantics.getCollectionType() ).isInstanceOf( CustomCollectionType.class );
			final CustomCollectionType collectionType = (CustomCollectionType) semantics.getCollectionType();
			assertThat( collectionType.getUserType() ).isInstanceOf( UniqueListType.class );
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

	@Test
	public void testBasicRegistrationUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final TheEntityWithUniqueListRegistration entity = new TheEntityWithUniqueListRegistration( 1, "first" );
			entity.setStrings( new ArrayList<>() );
			entity.getStrings().add( "the string" );
			entity.getStrings().add( "another" );
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final TheEntityWithUniqueListRegistration loaded = session.createQuery( "from TheEntityWithUniqueListRegistration", TheEntityWithUniqueListRegistration.class ).uniqueResult();
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
		scope.getSessionFactory().getSchemaManager().truncate();
	}

}
