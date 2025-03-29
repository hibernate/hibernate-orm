/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.json;

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
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonArray.class)
public class JsonArrayTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-array-example[]
			em.createQuery( "select json_array('val1', 'val2'), json_array(1, false, 'val')" ).getResultList();
			//end::hql-json-array-example[]
		} );
	}

	@Test
	public void testNullClause(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-array-on-null-example[]
			em.createQuery("select json_array(null, 1 null on null)" ).getResultList();
			//end::hql-json-array-on-null-example[]
		} );
	}

	@Test
	public void testAbsentOnNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.createQuery("select json_array(null, 1 absent on null)" ).getResultList();
		} );
	}

}
