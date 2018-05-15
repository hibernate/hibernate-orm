/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.PropertyValueException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11596")
public class OneToOneJoinTableNonOptionalTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Show.class, ShowDescription.class};
	}

	@Test
	public void testSavingEntitiesWithANullOneToOneAssociationValue() {
		doInHibernate( this::sessionFactory, session -> {
			Show show = new Show();
			session.save( show );
		} );

		try {
			doInHibernate( this::sessionFactory, session -> {
				ShowDescription showDescription = new ShowDescription();
				session.save( showDescription );
			} );
			fail();
		}
		catch (PropertyValueException expected) {
			assertTrue( expected.getMessage().startsWith( "not-null property references a null or transient value" ) );
		}
	}

	@Entity(name = "Show")
	@Table(name = "T_SHOW")
	public static class Show {

		@Id
		@GeneratedValue
		private Integer id;

		@OneToOne
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
		@GeneratedValue
		private Integer id;

		@OneToOne(mappedBy = "description", cascade = CascadeType.ALL, optional = false)
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
