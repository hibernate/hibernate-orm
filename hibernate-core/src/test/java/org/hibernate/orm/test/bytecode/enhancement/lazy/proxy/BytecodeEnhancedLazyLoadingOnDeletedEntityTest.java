/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.BytecodeEnhancedLazyLoadingOnDeletedEntityTest.*;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.LazyInitializationException;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@JiraKey("HHH-14811")
@DomainModel(
		annotatedClasses = {
				AssociationOwner.class, AssociationNonOwner.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = DEFAULT_LIST_SEMANTICS, value = "BAG" ),
		}
)
@SessionFactory(
		// This test only makes sense if association properties *can* be uninitialized.
		applyCollectionsInDefaultFetchGroup = false
)
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class BytecodeEnhancedLazyLoadingOnDeletedEntityTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void accessUnloadedLazyAssociationOnDeletedOwner(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			AssociationOwner owner = new AssociationOwner();
			owner.setId( 1 );
			for ( int i = 0; i < 2; i++ ) {
				AssociationNonOwner nonOwner = new AssociationNonOwner();
				nonOwner.setId( i );
				s.persist( nonOwner );
				nonOwner.getOwners().add( owner );
				owner.getNonOwners().add( nonOwner );
			}
			s.persist( owner );
		} );
		assertThatThrownBy( () -> scope.inTransaction( session -> {
			AssociationOwner owner = session.getReference( AssociationOwner.class, 1 );
			session.remove( owner );
			session.flush();
			owner.getNonOwners().size();
		} ) )
				.isInstanceOf( LazyInitializationException.class )
				.hasMessageContaining(
						"Could not locate EntityEntry for the collection owner in the PersistenceContext" );
	}

	@Test
	public void accessUnloadedLazyAssociationOnDeletedNonOwner(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			AssociationNonOwner nonOwner = new AssociationNonOwner();
			nonOwner.setId( 1 );
			s.persist( nonOwner );
		} );
		assertThatThrownBy( () -> scope.inTransaction( session -> {
			AssociationNonOwner nonOwner = session.getReference( AssociationNonOwner.class, 1 );
			session.remove( nonOwner );
			session.flush();
			nonOwner.getOwners().size();
		} ) )
				.isInstanceOf( LazyInitializationException.class )
				.hasMessageContaining(
						"Could not locate EntityEntry for the collection owner in the PersistenceContext" );
	}

	@Entity(name = "AOwner")
	@Table
	static class AssociationOwner {

		@Id
		Integer id;

		@ManyToMany(fetch = FetchType.LAZY)
		List<AssociationNonOwner> nonOwners = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<AssociationNonOwner> getNonOwners() {
			return nonOwners;
		}

		public void setNonOwners(
				List<AssociationNonOwner> nonOwners) {
			this.nonOwners = nonOwners;
		}
	}

	@Entity(name = "ANonOwner")
	@Table
	static class AssociationNonOwner {

		@Id
		Integer id;

		@ManyToMany(mappedBy = "nonOwners", fetch = FetchType.LAZY)
		List<AssociationOwner> owners = new ArrayList<>();

		AssociationNonOwner() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<AssociationOwner> getOwners() {
			return owners;
		}

		public void setOwners(List<AssociationOwner> owners) {
			this.owners = owners;
		}
	}
}
