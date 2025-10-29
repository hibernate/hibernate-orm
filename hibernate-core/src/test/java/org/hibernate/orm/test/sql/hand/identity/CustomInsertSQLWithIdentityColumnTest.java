/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.identity;


import org.hibernate.JDBCException;
import org.hibernate.orm.test.sql.hand.Organization;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Custom SQL tests for combined usage of custom insert SQL and identity columns
 *
 * @author Gail Badner
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/sql/hand/identity/Mappings.hbm.xml"
)
@SessionFactory
public class CustomInsertSQLWithIdentityColumnTest {

	@Test
	public void testBadInsertionFails(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Organization org = new Organization( "hola!" );
					// expecting bad custom insert statement to fail
					assertThrows( JDBCException.class, () -> {
						session.persist( org );
						session.remove( org );
					} );
				}
		);
	}
}
