/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@ServiceRegistry
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@SessionFactory
public class LiteralTests {

	@Test
	public void testJdbcTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t 12:30:00}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t '12:30:00'}" ).list();
				}
		);
	}

	@Test
	public void testJdbcDateLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d 1999-12-31}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d '1999-12-31'}" ).list();
				}
		);
	}

	@Test
	public void testJdbcTimestampLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {ts 1999-12-31 12:30:00}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {ts '1999-12-31 12:30:00'}" ).list();
				}
		);
	}

	@Test
	public void testLocalDateLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDate = {1999-12-31}" ).list();
				}
		);
	}

	@Test
	public void testLocalTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalTime = {12:59:59}" ).list();
				}
		);
	}

	@Test
	public void testDateTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// todo (6.0) : removed this difference between the string-literal form and the date-time-field form (with/without 'T')

					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDateTime = {1999-12-31 12:59:59}" ).list();

					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 +01:00}" ).list();

					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 CST}" ).list();
				}
		);
	}

	@Test
	public void isolated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDateTime = {1999-12-31 12:59:59}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 +01:00}" ).list();
				}
		);
	}
}
