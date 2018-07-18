/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.exception;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.fail;

@TestForIssue(jiraKey = "HHH-12666")
public class ConstraintViolationExceptionConversionTest extends BaseExceptionConversionTest {

	public ConstraintViolationExceptionConversionTest(BootstrapMethod bootstrapMethod,
			ExceptionConversionSetting exceptionConversionSetting,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionConversionSetting, exceptionExpectations );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				AInfo.class
		};
	}

	@Test
	public void testConstraintViolationOnSave() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		s.flush();
		s.clear();
		try {
			AInfo anotherAInfo = new AInfo();
			anotherAInfo.uniqueString = "unique";
			s.save( anotherAInfo );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onConstraintViolationOnSaveAndSaveOrUpdate( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testConstraintViolationOnSaveOrUpdate() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		s.flush();
		s.clear();
		try {
			AInfo anotherAInfo = new AInfo();
			anotherAInfo.uniqueString = "unique";
			s.saveOrUpdate( anotherAInfo );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onConstraintViolationOnSaveAndSaveOrUpdate( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testConstraintViolationOnPersist() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
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
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testConstraintViolationOnMerge() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		s.flush();
		s.clear();
		try {
			AInfo anotherAInfo = new AInfo();
			anotherAInfo.uniqueString = "unique";
			s.merge( anotherAInfo );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onConstraintViolationOnPersistAndMergeAndFlush( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testConstraintViolationUpdateFlush() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		AInfo aInfo1 = new AInfo();
		s.persist( aInfo1 );
		s.flush();
		s.clear();
		try {
			aInfo1 = s.get( AInfo.class, aInfo1.id );
			aInfo1.uniqueString = "unique";
			s.flush();
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onConstraintViolationOnPersistAndMergeAndFlush( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
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
