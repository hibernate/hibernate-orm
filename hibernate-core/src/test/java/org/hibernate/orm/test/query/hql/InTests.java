/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.dialect.DerbyDialect;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class InTests {

	@Test
    @Jira("https://hibernate.atlassian.net/browse/HHH-19698")
    @SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't like null literals in the in predicate")
	public void testInWithNullLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
                    assertEquals(0, session.createQuery("select 1 where 1 in (null)", Integer.class).list().size());
				}
		);
	}

}
