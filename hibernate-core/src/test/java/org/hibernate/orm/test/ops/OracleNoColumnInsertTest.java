/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ops;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = OracleDialect.class)
@JiraKey(value = "HHH-13104")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ops/Competition.hbm.xml"
)
@SessionFactory
public class OracleNoColumnInsertTest {

	@Test
	public void test(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> {
			Competition competition = new Competition();

			session.persist( competition );
		} );
	}
}
