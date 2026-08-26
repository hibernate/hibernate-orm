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

import static org.hibernate.orm.test.function.json.JsonTestHelper.assertNoJsonInjection;

/**
 * @author Christian Beikov
 */
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonReplace.class)
public class JsonReplaceTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-replace-example[]
			em.createQuery( "select json_replace('{\"a\":1}', '$.a', 2)" ).getResultList();
			//end::hql-json-replace-example[]
		} );
	}

	@Test
	public void testPathInjection(SessionFactoryScope scope) {
		scope.inSession( em -> {
			try {
				em.createQuery( "select json_replace('{\"a\":1}', :path, 1)", Tuple.class )
						.setParameter( "path", "$'--" )
						.getResultList();
			}
			catch ( RuntimeException e ) {
				assertNoJsonInjection( e );
			}
		} );
	}

}
