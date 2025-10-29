/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.quotedidentifiers;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.orm.junit.DialectFeatureCheck;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Test of various situations with native-sql queries and quoted identifiers
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = NativeSqlAndQuotedIdentifiersTest.LocalDialectCheck.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/sql/hand/quotedidentifiers/Mappings.hbm.xml"
)
@SessionFactory
public class NativeSqlAndQuotedIdentifiersTest {

	public static class LocalDialectCheck implements DialectFeatureCheck {

		@Override
		public boolean apply(Dialect dialect) {
			return '\"' == dialect.openQuote();
		}
	}

	@BeforeAll
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				session ->
						session.persist( new Person( "me" ) )
		);
	}

	@AfterAll
	protected void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testCompleteScalarDiscovery(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.getNamedQuery( "query-person" ).list()
		);
	}

	@Test
	public void testPartialScalarDiscovery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.getNamedNativeQuery( "query-person", "person-scalar" );
					query.list();
				}
		);
	}

	@Test
	public void testBasicEntityMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.getNamedNativeQuery( "query-person", "person-entity-basic" );
					query.list();
				}
		);
	}

	@Test
	public void testExpandedEntityMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.getNamedNativeQuery( "query-person", "person-entity-expanded" );
					query.list();
				}
		);
	}
}
