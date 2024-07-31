/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.hibernate.Interceptor;
import org.hibernate.SessionBuilder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.type.Type;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernateSessionBuilder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@TestForIssue(jiraKey = "HHH-12718")
public class PreUpdateDirtyCheckingInterceptorTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class };
	}

	@Test
	public void testPreUpdateModifications() {
		Person person = new Person();

		doInHibernate( this::sessionFactory, session -> {
			session.persist( person );
		} );

		doInHibernateSessionBuilder( this::sessionWithInterceptor, session -> {
			Person p = session.find( Person.class, person.id );
			assertNotNull( p );
			assertNotNull( p.createdAt );
			assertNull( p.lastUpdatedAt );

			p.setName( "Changed Name" );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Person p = session.find( Person.class, person.id );
			assertNotNull( p.lastUpdatedAt );
		} );
	}

	public static class OnFlushDirtyInterceptor implements Interceptor {

		private static OnFlushDirtyInterceptor INSTANCE = new OnFlushDirtyInterceptor();

		@Override
		public int[] findDirty(
				Object entity,
				Object id,
				Object[] currentState,
				Object[] previousState,
				String[] propertyNames,
				Type[] types) {
			int[] result = new int[propertyNames.length];
			int span = 0;

			for ( int i = 0; i < previousState.length; i++ ) {
				if( !Objects.deepEquals(previousState[i], currentState[i])) {
					result[span++] = i;
				}
			}

			return result;
		}
	}

	private SessionBuilder sessionWithInterceptor() {
		return sessionFactory()
				.withOptions()
				.interceptor( OnFlushDirtyInterceptor.INSTANCE );
	}

	@Entity(name = "Person")
	@DynamicUpdate
	private static class Person {
		@Id
		@GeneratedValue
		private int id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		private Instant createdAt;

		public Instant getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Instant createdAt) {
			this.createdAt = createdAt;
		}

		private Instant lastUpdatedAt;

		public Instant getLastUpdatedAt() {
			return lastUpdatedAt;
		}

		public void setLastUpdatedAt(Instant lastUpdatedAt) {
			this.lastUpdatedAt = lastUpdatedAt;
		}

		@ElementCollection
		private List<String> tags;

		@Lob
		@Basic(fetch = FetchType.LAZY)
		private byte[] image;

		@PrePersist
		void beforeCreate() {
			this.setCreatedAt( Instant.now() );
		}

		@PreUpdate
		void beforeUpdate() {
			this.setLastUpdatedAt( Instant.now() );
		}
	}
}
