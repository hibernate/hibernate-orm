/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.json;

import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

/**
 * @author Christian Beikov
 */
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonObjectAgg.class)
public class JsonObjectAggregateTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-objectagg-example[]
			em.createQuery( "select json_objectagg(e.theString value e.id) from EntityOfBasics e" ).getResultList();
			//end::hql-json-objectagg-example[]
		} );
	}

	@Test
	public void testNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-objectagg-null-example[]
			em.createQuery( "select json_objectagg(e.theString : e.id null on null) from EntityOfBasics e" ).getResultList();
			//end::hql-json-objectagg-null-example[]
		} );
	}

	@Test
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true, reason = "MySQL has no way to throw an error on duplicate json object keys. The last one always wins.")
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "SQL Server has no way to throw an error on duplicate json object keys.")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA has no way to throw an error on duplicate json object keys.")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 has no way to throw an error on duplicate json object keys.")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB has no way to throw an error on duplicate json object keys.")
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, majorVersion = 15, matchSubTypes = true, reason = "CockroachDB has no way to throw an error on duplicate json object keys.")
	public void testUniqueKeys(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-objectagg-unique-keys-example[]
			em.createQuery( "select json_objectagg(e.theString : e.id with unique keys) from EntityOfBasics e" ).getResultList();
			//end::hql-json-objectagg-unique-keys-example[]
		} );
	}

}
