/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import java.sql.Statement;

import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(MySQLDialect.class)
@JiraKey( value = "HHH-1237")
@SessionFactory
@DomainModel
public class MySQLSetVariableEscapeColonTest {
	@Test
	public void testBoundedLongStringAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.doWork(
							(connection) -> {
								try ( final Statement statement = connection.createStatement() ) {
									statement.executeUpdate( "SET @a='test'" );
								}
							}
					);
					Object result = session.createNativeQuery( "SELECT @a FROM dual" ).uniqueResult();
					assertEquals("test", result);
					result = session.createNativeQuery( "SELECT @a::=20 FROM dual" ).uniqueResult();
					assertEquals(20, ((Number) result).intValue());
				}
		);
	}

}

