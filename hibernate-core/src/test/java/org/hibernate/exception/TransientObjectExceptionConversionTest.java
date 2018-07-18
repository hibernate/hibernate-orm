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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.fail;

@TestForIssue(jiraKey = "HHH-12666")
public class TransientObjectExceptionConversionTest extends BaseExceptionConversionTest {

	public TransientObjectExceptionConversionTest(BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				AInfo.class
		};
	}

	@Test
	public void testSave() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.save( a );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onTransientObjectOnSaveAndSaveOrUpdate( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testSaveOrUpdate() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.saveOrUpdate( a );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onTransientObjectOnSaveAndSaveOrUpdate( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testPersist() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.persist( a );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onTransientObjectOnPersistAndMergeAndFlush( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testMerge() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.merge( a );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onTransientObjectOnPersistAndMergeAndFlush( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testUpdateFlush() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.update( a );
			s.flush();
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onTransientObjectOnPersistAndMergeAndFlush( expected );
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
		@GeneratedValue
		private long id;
	}
}
