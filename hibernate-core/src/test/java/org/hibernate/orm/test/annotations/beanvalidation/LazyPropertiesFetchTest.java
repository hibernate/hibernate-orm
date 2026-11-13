/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Size;

@DomainModel(annotatedClasses = {
		LazyPropertiesFetchTest.Association.class,
		LazyPropertiesFetchTest.MutableEntity.class
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.JAKARTA_VALIDATION_MODE, value = "CALLBACK"))
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey("HHH-19203")
class LazyPropertiesFetchTest {

	@AfterAll
	static void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testLazyCollectionNotFetched(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MutableEntity mutableEntity = new MutableEntity();
			mutableEntity.id = 1L;
			mutableEntity.lazyCollection = List.of( 1, 2 );
			session.persist( mutableEntity );
		} );

		SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			MutableEntity fetched = session.find( MutableEntity.class, 1L );
			inspector.assertExecutedCount( 1 );
			fetched.mutableField = 1;
		} );

		inspector.assertExecutedCount( 2 );
	}

	@Test
	void testLazyCollectionFetchDoesntDependOnEachOther(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MutableEntity mutableEntity = new MutableEntity();
			mutableEntity.id = 2L;
			mutableEntity.lazyCollection = List.of( 1, 2 );

			Association asso = new Association();
			asso.id = 1L;
			asso.lazyCollection = List.of( 2, 3 );

			mutableEntity.lazyAssociation = List.of( asso );

			session.persist( mutableEntity );
		} );

		SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			MutableEntity fetched = session.find( MutableEntity.class, 2L );
			inspector.assertExecutedCount( 1 );

			Association asso = fetched.lazyAssociation.get( 0 );
			inspector.assertExecutedCount( 2 );

			asso.mutableField = 5;
		} );
		inspector.assertExecutedCount( 3 );
	}

	@Entity(name = "MutableEntity")
	static class MutableEntity {
		@Id
		private Long id;

		private int mutableField = 0;

		@Size(max = 10)
		@ElementCollection
		@CollectionTable(name = "LazyPropertiesCollection1")
		private List<Integer> lazyCollection;

		@OneToMany(cascade = CascadeType.PERSIST)
		private List<Association> lazyAssociation;
	}

	@Entity(name = "Association")
	static class Association {
		@Id
		private Long id;

		private int mutableField = 0;

		@Size(max = 10)
		@ElementCollection
		@CollectionTable(name = "LazyPropertiesCollection2")
		private List<Integer> lazyCollection;
	}
}
