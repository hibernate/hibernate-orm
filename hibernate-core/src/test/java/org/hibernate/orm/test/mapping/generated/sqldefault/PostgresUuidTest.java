/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.sqldefault;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@SessionFactory
@DomainModel(annotatedClasses = PostgresUuidTest.It.class)
@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 13)
public class PostgresUuidTest {

	@Test
	void testit(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.persist( new It() ) );
		scope.inTransaction( s -> {
			UUID uuid = s.createQuery("select uuid from It", UUID.class).getSingleResult();
			String string = s.createQuery("select str(uuid) from It", String.class).getSingleResult();
			Assertions.assertEquals( string, uuid.toString() );
		});
	}

	@Entity(name="It")
	public static class It {
		@Id @Generated
		@ColumnDefault("gen_random_uuid()")
		UUID uuid;
	}
}
