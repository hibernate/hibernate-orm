/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@JiraKey(value = "HHH-12666")
@RequiresDialect(H2Dialect.class)
public class ConstraintViolationExceptionHandlingTest extends BaseExceptionHandlingTest {

	public ConstraintViolationExceptionHandlingTest(BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class, AInfo.class };
	}

	@Test
	public void testConstraintViolationOnSave() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			var aInfo = new AInfo();
			aInfo.uniqueString = "unique";
			s.persist( aInfo );
			s.flush();
			s.clear();
			try {
				AInfo anotherAInfo = new AInfo();
				anotherAInfo.uniqueString = "unique";
				s.persist( anotherAInfo );
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onConstraintViolationOnSaveAndSaveOrUpdate( expected );
			}
		} );
	}

	@Test
	public void testConstraintViolationOnPersist() {
		TransactionUtil.inTransaction(  sessionFactory(), (s) -> {
			var aInfo = new AInfo();
			aInfo.uniqueString = "unique";
			s.persist( aInfo );
			s.flush();
			s.clear();
			try {
				AInfo anotherAInfo = new AInfo();
				anotherAInfo.uniqueString = "unique";
				s.persist( anotherAInfo );
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onConstraintViolationOnPersistAndMergeAndFlush( expected );
			}
		} );
	}

	@Test
	public void testConstraintViolationOnMerge() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			AInfo aInfo = new AInfo();
			aInfo.uniqueString = "unique";
			s.persist( aInfo );
			s.flush();
			s.clear();
			try {
				var anotherAInfo = new AInfo();
				anotherAInfo.uniqueString = "unique";
				s.merge( anotherAInfo );
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onConstraintViolationOnPersistAndMergeAndFlush( expected );
			}
		} );
	}

	@Test
	public void testConstraintViolationUpdateFlush() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			AInfo aInfo = new AInfo();
			aInfo.uniqueString = "unique";
			s.persist( aInfo );
			AInfo aInfo1 = new AInfo();
			s.persist( aInfo1 );
			s.flush();
			s.clear();
			try {
				aInfo1 = s.find( AInfo.class, aInfo1.id );
				aInfo1.uniqueString = "unique";
				s.flush();
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onConstraintViolationOnPersistAndMergeAndFlush( expected );
			}
		} );
	}

	@Entity(name = "A")
	public static class A {
		@Id
		private long id;

		@ManyToOne(optional = false)
		private AInfo aInfo;
	}

	@Entity(name = "AInfo")
	public static class AInfo {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private long id;

		@Column(unique = true)
		private String uniqueString;
	}
}
