/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml.sequences;

import jakarta.persistence.EntityManager;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSequences.class )
@Jpa(
		xmlMappings = {"org/hibernate/orm/test/jpa/xml/sequences/orm.xml", "org/hibernate/orm/test/jpa/xml/sequences/orm2.xml"}
)
public class XmlTest {
	@Test
	public void testXmlMappingCorrectness(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.close();
	}
}
