/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.xml;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Christian Beikov
 */
@DomainModel
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.XML_FUNCTIONS_ENABLED, value = "true"))
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsXmlpi.class)
public class XmlPiTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-xmlpi-example[]
			em.createQuery( "select xmlpi(name php)" ).getResultList();
			//end::hql-xmlpi-example[]
		} );
	}

	@Test
	public void testContent(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-xmlpi-content-example[]
			em.createQuery("select xmlpi(name `php`, 'echo \"test\"')" ).getResultList();
			//end::hql-xmlpi-content-example[]
		} );
	}

}
