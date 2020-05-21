/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Timestamp;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-14036")
@RequiresDialect(PostgreSQL10Dialect.class)
public class PostgreSQL10TimezoneFunctionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityTest.class };
	}

	@Test
	public void testTimezone() {
		inTransaction(
				session -> {
					QueryImplementor query = session.createQuery( "select timezone(?1,stamp) from EntityTest " );
					query.setParameter( 1, "Central European Standard Time" );
					List list = query.list();
				}
		);
	}

	@Entity(name = "EntityTest")
	public static class EntityTest {
		@Id
		@GeneratedValue
		private Long id;

		private Timestamp stamp;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Timestamp getStamp() {
			return stamp;
		}

		public void setStamp(Timestamp stamp) {
			this.stamp = stamp;
		}
	}
}
