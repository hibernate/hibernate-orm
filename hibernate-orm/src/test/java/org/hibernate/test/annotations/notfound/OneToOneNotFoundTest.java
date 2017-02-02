/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.notfound;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11591")
public class OneToOneNotFoundTest extends BaseCoreFunctionalTestCase {

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Show.class, ShowDescription.class};
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				connection.createStatement().execute("create table SHOW_DESCRIPTION ( ID integer not null, primary key (ID) )" );
				connection.createStatement().execute("create table T_SHOW ( id integer not null, primary key (id) )" );
				connection.createStatement().execute("create table TSHOW_SHOWDESCRIPTION ( DESCRIPTION_ID integer, SHOW_ID integer not null, primary key (SHOW_ID) )" );

			} );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Show show = new Show();
			show.setId( 1 );
			ShowDescription showDescription = new ShowDescription();
			showDescription.setId( 2 );
			show.setDescription( showDescription );
			session.save( showDescription );
			session.save( show );

		} );

		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				connection.createStatement().execute( "delete from SHOW_DESCRIPTION where ID = 2" );

			} );
		} );
	}

	@After
	public void tearDow() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				connection.createStatement().execute( "drop table TSHOW_SHOWDESCRIPTION" );
				connection.createStatement().execute( "drop table SHOW_DESCRIPTION" );
				connection.createStatement().execute( "drop table T_SHOW" );

			} );
		} );
	}

	@Test
	public void testOneToOne() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			final Show show2 = session.find( Show.class, 1 );
			assertNotNull( show2 );
			assertNull( show2.getDescription() );
		} );
	}

	@Entity(name = "Show")
	@Table(name = "T_SHOW")
	public static class Show {

		@Id
		private Integer id;

		@OneToOne
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinTable(name = "TSHOW_SHOWDESCRIPTION",
				joinColumns = @JoinColumn(name = "SHOW_ID"),
				inverseJoinColumns = @JoinColumn(name = "DESCRIPTION_ID"), foreignKey = @ForeignKey(name = "FK_DESC"))
		private ShowDescription description;


		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ShowDescription getDescription() {
			return description;
		}

		public void setDescription(ShowDescription description) {
			this.description = description;
			description.setShow( this );
		}
	}

	@Entity(name = "ShowDescription")
	@Table(name = "SHOW_DESCRIPTION")
	public static class ShowDescription {

		@Id
		@Column(name = "ID")
		private Integer id;

		@NotFound(action = NotFoundAction.IGNORE)
		@OneToOne(mappedBy = "description", cascade = CascadeType.ALL)
		private Show show;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Show getShow() {
			return show;
		}

		public void setShow(Show show) {
			this.show = show;
		}
	}
}
