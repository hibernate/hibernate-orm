/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDialect(DB2Dialect.class)
@SessionFactory
public class DdlTypeRegistryTest {

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20259")
	public void testKeepBiggest(SessionFactoryScope scope) {
		final DdlTypeRegistry ddlTypeRegistry = scope.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry();
		assertThat( ddlTypeRegistry.getSqlTypeCode( "smallint" ) ).isEqualTo( SqlTypes.SMALLINT );
	}

}
