/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Strong Liu
 */
@JiraKeyGroup( value = {
		@JiraKey( value = "HHH-6039" ),
		@JiraKey( value = "HHH-6100" )
} )
@Jpa(
		xmlMappings = {"org/hibernate/orm/test/jpa/xml/Qualifier.hbm.xml"}
)
public class JpaEntityNameTest {

	@Test
	public void testUsingSimpleHbmInJpa(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Qualifier> cq = cb.createQuery(Qualifier.class);
					Root<Qualifier> qualifRoot = cq.from(Qualifier.class);
					cq.where( cb.equal( qualifRoot.get( "qualifierId" ), 32l ) );
					entityManager.createQuery(cq).getResultList();
				}
		);
	}
}
