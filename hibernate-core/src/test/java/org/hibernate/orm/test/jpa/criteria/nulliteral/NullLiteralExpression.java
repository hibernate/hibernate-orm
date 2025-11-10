/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.nulliteral;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@DomainModel(annotatedClasses = { Person.class, Subject.class })
@SessionFactory
public class NullLiteralExpression {
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-11159" )
	public void testNullLiteralExpressionInCriteriaUpdate(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaUpdate<Person> criteriaUpdate = builder.createCriteriaUpdate( Person.class );
			criteriaUpdate.from( Person.class );
			criteriaUpdate.set( Person_.subject, builder.nullLiteral( Subject.class ) );
			entityManager.createQuery( criteriaUpdate ).executeUpdate();
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16803" )
	public void testEnumNullLiteralUpdate(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaUpdate<Person> criteriaUpdate = builder.createCriteriaUpdate( Person.class );
			criteriaUpdate.from( Person.class );
			criteriaUpdate.set( Person_.eyeColor, builder.nullLiteral( EyeColor.class ) );
			entityManager.createQuery( criteriaUpdate ).executeUpdate();
		} );
	}
}
