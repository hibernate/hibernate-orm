/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

/**
 * @author Christian Beikov
 */
@DomainModel(standardModels = StandardDomainModel.CONTACTS)
@SessionFactory
@JiraKey("HHH-17412")
public class CriteriaExpressionAsValueTests {

	@Test
	public void testParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					cq.multiselect(
							root.get( "id" ),
							root.get( "name" )
					);
					final JpaParameterExpression<Contact.Gender> parameter = cb.parameter( Contact.Gender.class );
					cq.where( root.get( "gender" ).equalTo( parameter ) );

					session.createQuery( cq ).setParameter( parameter, Contact.Gender.FEMALE ).getResultList();
				}
		);
	}
}
