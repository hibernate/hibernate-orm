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
 * @author Gavin King
 */
@SuppressWarnings("WeakerAccess")
@ServiceRegistry
@DomainModel( standardModels = StandardDomainModel.HELPDESK )
@SessionFactory
public class InsertUpdateTests {

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("update Ticket set subject = 'Outage', details = 'The service is down' where id = 1").executeUpdate();
					session.createQuery("update Ticket t set t.subject = 'Outage' where t.id = 1").executeUpdate();
					session.createQuery("update Ticket t set t.subject = upper(t.subject) where t.id = 1").executeUpdate();
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("delete from Ticket where id = 1").executeUpdate();
					session.createQuery("delete from Ticket t where t.id = 1").executeUpdate();
				}
		);
	}

	@Test
	public void testInsertSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("delete from Ticket").executeUpdate();
					session.createQuery("insert into Ticket (id, key, subject, details) select 6, 'ABC123', 'Outage', 'Something is broken'").executeUpdate();
					session.createQuery("insert into Ticket (id, key, subject, details) select 13, 'DEF456', tt.subject, tt.details from Ticket tt").executeUpdate();
				}
		);
	}

	@Test
	public void testAliasedInsertSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("delete from Ticket").executeUpdate();
					session.createQuery("insert into Ticket t (t.id, t.key, t.subject, t.details) select 12, 'ABC123', 'Outage', 'Something is broken'").executeUpdate();
					session.createQuery("insert into Ticket as t (t.id, t.key, t.subject, t.details) select 5, 'DEF456', tt.subject, tt.details from Ticket tt").executeUpdate();
				}
		);
	}
}
