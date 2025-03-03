/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.customsql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = CustomSqlRestrictionOverridesTest.Secure.class)
@RequiresDialect(H2Dialect.class)
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(SQLServerDialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(value = DB2Dialect.class, majorVersion = 11)
@RequiresDialect(OracleDialect.class)
public class CustomSqlRestrictionOverridesTest {
	@Test
	public void testCustomSql(SessionFactoryScope scope) throws NoSuchAlgorithmException {
		Secure sec1 = new Secure();
		sec1.hash = MessageDigest.getInstance( "SHA-256" ).digest( "hello".getBytes() );
		scope.inTransaction( s -> s.persist( sec1 ) );
		Secure sec2 = new Secure();
		sec2.hash = MessageDigest.getInstance( "SHA-256" ).digest( "not hello".getBytes() );
		scope.inTransaction( s -> s.persist( sec2 ) );
		Secure secure1 = scope.fromTransaction( s -> s.find( Secure.class, sec1.id ) );
		assertNotNull( secure1 );
		Secure secure2 = scope.fromTransaction( s -> s.find( Secure.class, sec2.id ) );
		assertNull( secure2 );
	}

	@Entity
	@Table(name = "SecureTable")
	@DialectOverride.SQLRestriction(dialect = H2Dialect.class,
			override = @SQLRestriction("hash = hash('SHA-256', 'hello')"))
	@DialectOverride.SQLRestriction(dialect = MySQLDialect.class,
			override = @SQLRestriction("hash = unhex(sha2('hello', 256))"))
	@DialectOverride.SQLRestriction(dialect = PostgreSQLDialect.class,
			override = @SQLRestriction("hash = sha256('hello')"))
	@DialectOverride.SQLRestriction(dialect = SQLServerDialect.class,
			override = @SQLRestriction("hash = hashbytes('SHA2_256', 'hello')"))
	@DialectOverride.SQLRestriction(dialect = DB2Dialect.class,
			override = @SQLRestriction("hash = hash('hello', 2)"))
	@DialectOverride.SQLRestriction(dialect = OracleDialect.class,
			override = @SQLRestriction("hash = standard_hash('hello', 'SHA256')"))
	static class Secure {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;
		byte[] hash;
	}
}
