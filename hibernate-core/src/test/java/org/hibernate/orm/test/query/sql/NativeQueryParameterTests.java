/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.persistence.TemporalType;

import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.HELPDESK )
@SessionFactory
public class NativeQueryParameterTests {
	@Test
	public void testBasicParameterBinding(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeQuery( "select t.id, t.key, t.subject from ticket t where t.key = ?" )
							.setParameter( 1, "ABC-123" )
							.list();
				}
		);
	}

	@Test
	public void testTypedParameterBinding(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeQuery( "select t.id, t.key, t.subject from ticket t where t.key = ?" )
							.setParameter( 1, "ABC-123", StandardBasicTypes.STRING )
							.list();
				}
		);
	}

	@Test
	public void testTemporalParameterBinding(SessionFactoryScope scope) {
		final String qryString = "select i.id, i.effectiveStart, i.effectiveEnd " +
				"  from incident i" +
				"  where i.reported BETWEEN ? AND ?";

		scope.inTransaction(
				session -> {
					{
						final Instant now = Instant.now();
						final Instant startPeriod = now.minus( 30, ChronoUnit.DAYS );

						session.createNativeQuery( qryString )
								.setParameter( 1, startPeriod )
								.setParameter( 2, now )
								.list();
					}

					{
						final Instant now = Instant.now();
						final Instant startPeriod = now.minus( 30, ChronoUnit.DAYS );

						session.createNativeQuery( qryString )
								.setParameter( 1, startPeriod, TemporalType.DATE )
								.setParameter( 2, now, TemporalType.DATE )
								.list();
					}

					{
						final Instant now = Instant.now();
						final Instant startPeriod = now.minus( 30, ChronoUnit.DAYS );

						session.createNativeQuery( qryString )
								.setParameter( 1, startPeriod, TemporalType.TIMESTAMP )
								.setParameter( 2, now, TemporalType.TIMESTAMP )
								.list();
					}

					{
						final Instant now = Instant.now();
						final Instant startPeriod = now.minus( 30, ChronoUnit.DAYS );

						session.createNativeQuery( qryString )
								.setParameter( 1, startPeriod, TemporalType.TIME )
								.setParameter( 2, now, TemporalType.TIME )
								.list();
					}
				}
		);
	}
}
