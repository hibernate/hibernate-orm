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
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsXmlelement.class)
public class XmlElementTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-xmlelement-example[]
			em.createQuery( "select xmlelement(name myelement)" ).getResultList();
			//end::hql-xmlelement-example[]
		} );
	}

	@Test
	public void testAttributesAndContent(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-xmlelement-attributes-content-example[]
			em.createQuery("select xmlelement(name `my-element`, xmlattributes(123 as attr1, '456' as `attr-2`), 'myContent', xmlelement(name empty))" ).getResultList();
			//end::hql-xmlelement-attributes-content-example[]
		} );
	}

}
