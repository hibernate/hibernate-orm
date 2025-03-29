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
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = CustomSqlOverrideTest.Custom.class)
@RequiresDialect(H2Dialect.class)
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 13)
@RequiresDialect(SQLServerDialect.class)
public class CustomSqlOverrideTest {
	@Test
	public void testCustomSql(SessionFactoryScope scope) {
		Custom c = new Custom();
		scope.inTransaction(s->{
			s.persist(c);
			c.whatever = "old value";
			s.flush();
			assertNotNull(c.id);
			assertNotNull(c.uid);
			s.clear();
			Custom cc = s.find(Custom.class, c.id);
			assertNotNull(cc.id);
			assertNotNull(cc.uid);
			assertEquals("old value", cc.whatever);
			cc.whatever = "new value";
			s.flush();
			s.clear();
			Custom ccc = s.find(Custom.class, c.id);
			assertNotNull(cc.id);
			assertNotNull(cc.uid);
			assertEquals("new value", ccc.whatever);
			assertEquals(cc.id, ccc.id);
			assertNotEquals(cc.uid, ccc.uid);
		});
	}
	@Entity
	@Table(name = "CustomTable")
//    @SQLInsert(sql="")
	@DialectOverride.SQLInsert(dialect = H2Dialect.class,
			override = @SQLInsert(sql="insert into CustomTable (uid,whatever) values (random_uuid(),?)"))
	@DialectOverride.SQLInsert(dialect = MySQLDialect.class,
			override = @SQLInsert(sql="insert into CustomTable (uid,whatever) values (uuid(),?)"))
	@DialectOverride.SQLInsert(dialect = PostgreSQLDialect.class,
			override = @SQLInsert(sql="insert into CustomTable (uid,whatever) values (gen_random_uuid(),?)"))
	@DialectOverride.SQLInsert(dialect = SQLServerDialect.class,
			override = @SQLInsert(sql="insert into CustomTable (uid,whatever) values (newid(),?)"))
//    @SQLUpdate(sql="")
	@DialectOverride.SQLUpdate(dialect = H2Dialect.class,
			override = @SQLUpdate(sql="update CustomTable set uid = random_uuid(), whatever = ? where id = ?"))
	@DialectOverride.SQLUpdate(dialect = MySQLDialect.class,
			override = @SQLUpdate(sql="update CustomTable set uid = uuid(), whatever = ? where id = ?"))
	@DialectOverride.SQLUpdate(dialect = PostgreSQLDialect.class,
			override = @SQLUpdate(sql="update CustomTable set uid = gen_random_uuid(), whatever = ? where id = ?"))
	@DialectOverride.SQLUpdate(dialect = SQLServerDialect.class,
			override = @SQLUpdate(sql="update CustomTable set uid = newid(), whatever = ? where id = ?"))
	static class Custom {
		@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;
		@Generated @Immutable
		String uid;
		String whatever;
	}
}
