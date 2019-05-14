/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;

import org.junit.jupiter.api.Test;

import java.time.*;


/**
 * @author Chris Cranford
 */
public class TimeTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityOfBasics.class,
		};
	}

	@Test
	public void testEntitySaving() {
		final EntityOfBasics entity = new EntityOfBasics();
		entity.setId(1);
		entity.setTheLocalDate(LocalDate.now());
		entity.setTheLocalTime(LocalTime.now());
		entity.setTheLocalDateTime(LocalDateTime.now());
		entity.setTheOffsetDateTime(OffsetDateTime.now());
		entity.setTheZonedDateTime(ZonedDateTime.now());

		inTransaction( session -> {
			session.save( entity );
		} );
		inTransaction( session -> {
			session.createQuery("from EntityOfBasics").list();
		} );

	}
}
