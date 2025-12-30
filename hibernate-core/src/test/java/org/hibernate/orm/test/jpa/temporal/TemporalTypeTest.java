/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.temporal;

import java.util.Calendar;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Brett Meyer
 */
@BootstrapServiceRegistry(
		javaServices =
		@BootstrapServiceRegistry.JavaService(role = FunctionContributor.class,
				impl = TemporalTypeTest.MyFunctionContributor.class)
)
@DomainModel(annotatedClasses = TemporalTypeTest.DataPoint.class)
@SessionFactory
public class TemporalTypeTest {

	@Test
	public void testTemporalType(SessionFactoryScope scope) {
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		scope.inTransaction(
				entityManager -> {
					DataPoint dp = new DataPoint();
					dp.date1 = date;
					dp.date2 = date;
					dp.calendar1 = calendar;
					dp.calendar2 = calendar;
					entityManager.persist( dp );
				}
		);

		doTest(scope, "date1", date);
		doTest(scope, "date1", calendar);
		doTest(scope, "date2", date);
		doTest(scope, "date2", calendar);

		doTest(scope, "calendar1", date);
		doTest(scope, "calendar1", calendar);
		doTest(scope, "calendar2", date);
		doTest(scope, "calendar2", calendar);
	}

	private void doTest(SessionFactoryScope scope, String property, Object obj) {
		doTest( scope, property, obj, TemporalType.DATE );
		doTest( scope, property, obj, TemporalType.TIMESTAMP );
	}

	private void doTest(SessionFactoryScope scope, String property, Object obj, TemporalType temporalType) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery("from DataPoint where " + property + " = :obj");
					if (obj instanceof Calendar) {
						query.setParameter("obj", (Calendar) obj, temporalType);
					}
					else {
						query.setParameter("obj", (Date) obj, temporalType);
					}
				}
		);
	}

	@Test
	public void testTemporalTypeCalendar(SessionFactoryScope scope) {
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		scope.inTransaction(
				entityManager -> {
					DataPoint dp = new DataPoint();
					dp.date1 = date;
					dp.date2 = date;
					dp.calendar1 = calendar;
					dp.calendar2 = calendar;
					entityManager.persist( dp );
				}
		);
//		 Trigger initialization of BasicType for the Calendar and GregorianCalendar Java type
		var typeConfiguration = scope.getSessionFactory().getTypeConfiguration();
		typeConfiguration.getBasicTypeForJavaType( Calendar.class );
		// Instead of doing this, it's also possible to provoke the bug by binding another calendar parameter
		typeConfiguration.getBasicTypeRegistry().getRegisteredType( "java.util.GregorianCalendar" );
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery("from DataPoint dp where my_function(:cal) = 1")
							.setParameter("cal", calendar, TemporalType.TIMESTAMP);
					query.getResultList();
				}
		);
	}



	@Entity(name = "DataPoint")
	public static class DataPoint {
		@Id @GeneratedValue
		public long id;

		@Temporal( TemporalType.DATE )
		public Date date1;

		@Temporal( TemporalType.TIMESTAMP )
		public Date date2;

		@Temporal( TemporalType.DATE )
		public Calendar calendar1;

		@Temporal( TemporalType.TIMESTAMP )
		public Calendar calendar2;
	}

	public static class MyFunctionContributor implements FunctionContributor {
		@Override
		public void contributeFunctions(FunctionContributions functionContributions) {
			functionContributions.getFunctionRegistry().patternDescriptorBuilder( "my_function", "1" )
					.setExactArgumentCount( 1 )
					.setInvariantType( functionContributions.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ) )
					.register();
		}
	}
}
