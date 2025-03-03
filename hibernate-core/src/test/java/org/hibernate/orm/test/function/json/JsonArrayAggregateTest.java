/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.json;

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
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonArrayAgg.class)
public class JsonArrayAggregateTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-arrayagg-example[]
			em.createQuery( "select json_arrayagg(e.theString) from EntityOfBasics e" ).getResultList();
			//end::hql-json-arrayagg-example[]
		} );
	}

	@Test
	public void testNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-arrayagg-null-example[]
			em.createQuery( "select json_arrayagg(e.theString null on null) from EntityOfBasics e" ).getResultList();
			//end::hql-json-arrayagg-null-example[]
		} );
	}

	@Test
	public void testOrderBy(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-arrayagg-order-by-example[]
			em.createQuery( "select json_arrayagg(e.theString order by e.id) from EntityOfBasics e" ).getResultList();
			//end::hql-json-arrayagg-order-by-example[]
		} );
	}

}
