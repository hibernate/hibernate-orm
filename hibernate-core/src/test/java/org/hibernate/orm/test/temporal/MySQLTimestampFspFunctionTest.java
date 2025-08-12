/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.temporal;

import java.sql.Time;
import java.sql.Timestamp;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner.
 */
@TestForIssue( jiraKey = "HHH-8401")
@RequiresDialect( value = MySQLDialect.class)
@ServiceRegistry
@DomainModel
@SessionFactory
public class MySQLTimestampFspFunctionTest {

	@Test
	public void testTimeStampFunctions(SessionFactoryScope scope) {
		// current_timestamp(), localtime(), and localtimestamp() are synonyms for now(),
		// which returns the time at which the statement began to execute.
		// the returned values for now(), current_timestamp(), localtime(), and
		// localtimestamp() should be the same.
		// sysdate() is the time at which the function itself is executed, so the
		// value returned for sysdate() should be different.
		scope.inSession(
				s -> {
					Query q = s.createQuery(
							"select now(), current_timestamp(), localtime(), localtimestamp(), sysdate()"
					);
					Object[] oArray = (Object[]) q.uniqueResult();
					for ( Object o : oArray ) {
						if ( o instanceof Timestamp) {
							( (Timestamp) o ).setNanos( 0 );
						}
					}
					final Timestamp now = (Timestamp) oArray[0];
					assertEquals( now, oArray[1] );
					assertTrue( JdbcTimeJavaType.INSTANCE.areEqual( new Time( now.getTime() ), (Time) oArray[2] ) );
					assertEquals( now, oArray[3] );
					assertTrue( now.compareTo( (Timestamp) oArray[4] ) <= 0 );
				}
		);
	}
}
