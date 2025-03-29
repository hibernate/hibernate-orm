/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.xml;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.testing.orm.domain.StandardDomainModel;
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
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.XML_FUNCTIONS_ENABLED, value = "true"))
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsXmlagg.class)
public class XmlAggTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-xmlagg-example[]
			em.createQuery( "select xmlagg(xmlelement(name a, e.theString) order by e.id) from EntityOfBasics e" ).getResultList();
			//end::hql-xmlagg-example[]
		} );
	}

}
