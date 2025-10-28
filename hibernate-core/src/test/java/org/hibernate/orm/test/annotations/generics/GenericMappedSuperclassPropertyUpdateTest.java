/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				GenericMappedSuperclassPropertyUpdateTest.CommonEntity.class,
				GenericMappedSuperclassPropertyUpdateTest.SpecificEntity.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-19872")
public class GenericMappedSuperclassPropertyUpdateTest implements SessionFactoryScopeAware {
	private SessionFactoryScope scope;

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("criteriaUpdateFieldSetters")
	void testGenericHierarchy(Consumer<UpdateContext> updater) {
		scope.inTransaction( session -> {
			SpecificEntity relative = new SpecificEntity();
			SpecificEntity base = new SpecificEntity();
			session.persist( relative );
			session.persist( base );

			final CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaUpdate<SpecificEntity> criteriaUpdate = cb.createCriteriaUpdate( SpecificEntity.class );
			Root<SpecificEntity> root = criteriaUpdate.from( SpecificEntity.class );

			updater.accept( new UpdateContext( criteriaUpdate, root, base.getId(), relative ) );
			criteriaUpdate.where( cb.equal( root.get( GenericMappedSuperclassPropertyUpdateTest_.SpecificEntity_.id ), base.getId() ) );

			int updates = session.createQuery( criteriaUpdate ).executeUpdate();
			session.refresh( base );

			assertThat( updates )
					.isEqualTo( 1L );
			assertThat( base.getRelative() )
					.isEqualTo( relative );
		} );

	}


	static class UpdateContext {
		final CriteriaUpdate<SpecificEntity> query;
		final Root<SpecificEntity> root;
		final Long id;
		final SpecificEntity relative;

		UpdateContext(CriteriaUpdate<SpecificEntity> query, Root<SpecificEntity> root, Long id, SpecificEntity relative) {
			this.query = query;
			this.root = root;
			this.id = id;
			this.relative = relative;
		}
	}

	static Stream<Arguments> criteriaUpdateFieldSetters() {
		Consumer<UpdateContext> updateUsingPath = context ->
				context.query.set( context.root.get( GenericMappedSuperclassPropertyUpdateTest_.SpecificEntity_.relative ), context.relative );
		Consumer<UpdateContext> updateUsingSingularAttribute = context ->
				context.query.set( GenericMappedSuperclassPropertyUpdateTest_.SpecificEntity_.relative, context.relative );
		Consumer<UpdateContext> updateUsingName = context ->
				context.query.set( GenericMappedSuperclassPropertyUpdateTest_.SpecificEntity_.RELATIVE, context.relative );
		return Stream.of(
				Arguments.of( updateUsingPath ),
				Arguments.of( updateUsingSingularAttribute ),
				Arguments.of( updateUsingName )
		);
	}

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	@MappedSuperclass
	public abstract static class CommonEntity<E extends CommonEntity<?>> {

		@Id
		@GeneratedValue
		private Long id;

		Long getId() {
			return id;
		}

		@ManyToOne
		@JoinColumn
		private E relative;

		void setRelative(E relative) {
			this.relative = relative;
		}

		E getRelative() {
			return relative;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CommonEntity<?> that = (CommonEntity<?>) o;
			return Objects.equals( id, that.id ) && Objects.equals( relative, that.relative );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, relative );
		}
	}

	@Entity(name = "SpecificEntity")
	public static class SpecificEntity extends CommonEntity<SpecificEntity> {
	}
}
