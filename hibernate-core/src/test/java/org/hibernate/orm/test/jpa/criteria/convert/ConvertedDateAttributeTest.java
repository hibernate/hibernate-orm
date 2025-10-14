/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.convert;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {Log.class})
@SessionFactory
@JiraKey(value = "HHH-19862")
public class ConvertedDateAttributeTest implements SessionFactoryScopeAware {
	private SessionFactoryScope scope;

	@ParameterizedTest
	@MethodSource("criteriaUpdateFieldSetters")
	public void testConvertedFieldUpdateUsingPath(Consumer<UpdateContext> criteriaUpdateFieldSetter) {

		scope.inTransaction( (session) -> {
			Log log = new Log();
			session.persist( log );

			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaUpdate<Log> query = cb.createCriteriaUpdate( Log.class );
			final Root<Log> root = query.from( Log.class );
			query.where( cb.equal( root.get( Log_.id ), log.getId() ) );
			Date update = new Date();
			criteriaUpdateFieldSetter.accept( new UpdateContext( query, root, update ) );

			int updates = session.createMutationQuery( query ).executeUpdate();
			session.refresh( log );

			assertEquals( 1, updates );
			assertEquals( log.getLastUpdate(), update );

		} );
	}

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	static class UpdateContext {
		final CriteriaUpdate<Log> query;
		final Root<Log> root;
		final Date lastUpdate;

		public UpdateContext(CriteriaUpdate<Log> query, Root<Log> root, Date lastUpdate) {
			this.query = query;
			this.root = root;
			this.lastUpdate = lastUpdate;
		}
	}

	static Stream<Arguments> criteriaUpdateFieldSetters() {
		Consumer<UpdateContext> updateUsingPath = context ->
				context.query.set( context.root.get( Log_.lastUpdate ), context.lastUpdate );
		Consumer<UpdateContext> updateUsingSingularAttribute = context ->
				context.query.set( Log_.lastUpdate, context.lastUpdate );
		Consumer<UpdateContext> updateUsingName = context ->
				context.query.set( Log_.LAST_UPDATE, context.lastUpdate );
		return Stream.of(
				Arguments.of( updateUsingPath ),
				Arguments.of( updateUsingSingularAttribute ),
				Arguments.of( updateUsingName )
		);
	}
}
