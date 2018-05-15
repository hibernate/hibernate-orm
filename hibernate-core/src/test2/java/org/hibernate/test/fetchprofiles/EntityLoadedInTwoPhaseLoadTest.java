/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchprofiles;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@TestForIssue( jiraKey = "HHH-12297")
public class EntityLoadedInTwoPhaseLoadTest extends BaseCoreFunctionalTestCase {

	static final String FETCH_PROFILE_NAME = "fp1";

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testIfAllRelationsAreInitialized() {
		long startId = this.createSampleData();
		sessionFactory().getStatistics().clear();
		try {
			Start start = this.loadStartWithFetchProfile( startId );
			@SuppressWarnings( "unused" )
			String value = start.getVia2().getMid().getFinish().getValue();
			assertEquals( 4, sessionFactory().getStatistics().getEntityLoadCount() );
		}
		catch (LazyInitializationException e) {
			fail( "Everything should be initialized" );
		}
	}

	public Start loadStartWithFetchProfile(long startId) {
		return doInHibernate( this::sessionFactory, session -> {
			session.enableFetchProfile( FETCH_PROFILE_NAME );
			return session.get( Start.class, startId );
		} );
	}

	private long createSampleData() {
		return doInHibernate( this::sessionFactory, session -> {
			Finish finish = new Finish( "foo" );
			Mid mid = new Mid( finish );
			Via2 via2 = new Via2( mid );
			Start start = new Start( null, via2 );

			session.persist( start );

			return start.getId();
		} );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Start.class,
				Mid.class,
				Finish.class,
				Via1.class,
				Via2.class
		};
	}

	@Entity(name = "FinishEntity")
	public static class Finish {

		@Id
		@GeneratedValue
		private long id;

		@Column(name = "value", nullable = false)
		private String value;

		public Finish() {
		}

		public Finish(String value) {
			this.value = value;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Entity(name = "MidEntity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Mid.class, association = "finish", mode = FetchMode.JOIN)
	})
	public static class Mid {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Finish finish;

		public Mid() {
		}

		public Mid(Finish finish) {
			this.finish = finish;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Finish getFinish() {
			return finish;
		}

		public void setFinish(Finish finish) {
			this.finish = finish;
		}

	}

	@Entity(name = "StartEntity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Start.class, association = "via1", mode = FetchMode.JOIN),
			@FetchProfile.FetchOverride(entity = Start.class, association = "via2", mode = FetchMode.JOIN)
	})
	public static class Start {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Via1 via1;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Via2 via2;

		public Start() {
		}

		public Start(Via1 via1, Via2 via2) {
			this.via1 = via1;
			this.via2 = via2;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Via1 getVia1() {
			return via1;
		}

		public void setVia1(Via1 via1) {
			this.via1 = via1;
		}

		public Via2 getVia2() {
			return via2;
		}

		public void setVia2(Via2 via2) {
			this.via2 = via2;
		}

	}

	@Entity(name = "Via1Entity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Via1.class, association = "mid", mode = FetchMode.JOIN)
	})
	public static class Via1 {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Mid mid;

		public Via1() {
		}

		public Via1(Mid mid) {
			this.mid = mid;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Mid getMid() {
			return mid;
		}

		public void setMid(Mid mid) {
			this.mid = mid;
		}

	}

	@Entity(name = "Via2Entity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Via2.class, association = "mid", mode = FetchMode.JOIN)
	})
	public static class Via2 {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Mid mid;

		public Via2() {
		}

		public Via2(Mid mid) {
			this.mid = mid;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Mid getMid() {
			return mid;
		}

		public void setMid(Mid mid) {
			this.mid = mid;
		}

	}
}
