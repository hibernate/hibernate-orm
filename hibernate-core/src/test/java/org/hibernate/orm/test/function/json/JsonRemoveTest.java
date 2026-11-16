/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.json;

import org.hibernate.cfg.QuerySettings;

import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.function.json.JsonTestHelper.assertNoInjection;

/**
 * @author Christian Beikov
 */
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonRemove.class)
public class JsonRemoveTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-remove-example[]
			em.createQuery( "select json_remove('{\"a\":1,\"b\":2}', '$.a')" ).getResultList();
			//end::hql-json-remove-example[]
		} );
	}

	@Test
	public void testPathInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_remove('{\"a\":1}', :path)", Tuple.class )
					.setParameter( "path", "$'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPathBackslashInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_remove('{\"a\":1}', :path)", Tuple.class )
					.setParameter( "path", "$\\'--" )
					.getResultList();
		} );
	}

}
