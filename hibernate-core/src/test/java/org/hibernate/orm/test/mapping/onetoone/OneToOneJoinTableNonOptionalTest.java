/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.PropertyValueException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11596")
@DomainModel(
		annotatedClasses = {
				OneToOneJoinTableNonOptionalTest.Show.class,
				OneToOneJoinTableNonOptionalTest.ShowDescription.class
		}
)
@SessionFactory
public class OneToOneJoinTableNonOptionalTest {
	@Test
	public void testSavingEntitiesWithANullOneToOneAssociationValue(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Show show = new Show();
					session.persist( show );
				}
		);

		try {
			scope.inTransaction(
					(session) -> {
						ShowDescription showDescription = new ShowDescription();
						session.persist( showDescription );
					}
			);
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
