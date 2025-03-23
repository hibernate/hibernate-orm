/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.relationship;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				JoinedInheritanceWithOneToManyTest.BuildingList.class,
				JoinedInheritanceWithOneToManyTest.BuildingListEntry.class,
				JoinedInheritanceWithOneToManyTest.BLEHome.class,
				JoinedInheritanceWithOneToManyTest.BLENonLiving.class,
		}
)
@SessionFactory
public class JoinedInheritanceWithOneToManyTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			BuildingList buildingList = new BuildingList();
			buildingList.setName( "ABC" );
			session.persist( buildingList );

			BLEHome home = new BLEHome();
			home.setHasCtv( 123 );
			home.setList( buildingList );
			buildingList.getEntries().add( home );
			session.persist( home );

			BLENonLiving nonLiving = new BLENonLiving();
			nonLiving.setDelayed( true );
			nonLiving.setList( buildingList );
			buildingList.getEntries().add( nonLiving );
			session.persist( nonLiving );
		} );

		scope.inTransaction( session -> {
			List<BuildingList> buildingLists = session.createQuery( "from BuildingList" ).getResultList();
			BuildingList buildingList = buildingLists.get( 0 );
			assertEquals( 2, buildingList.getEntries().size() );
		} );
	}

	@MappedSuperclass
	public static class DBObject {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq")
		protected Integer id;

		@Temporal(TemporalType.TIMESTAMP)
		protected Date correctDate;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Date getCorrectDate() {
			return correctDate;
		}

		public void setCorrectDate(Date correctDate) {
			this.correctDate = correctDate;
		}
	}

	@Entity(name = "BuildingList")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "TB_BUILDING_LIST")
	@SequenceGenerator(name = "seq",
			sequenceName = "sq_building_list_id",
			allocationSize = 1)
	public static class BuildingList extends DBObject implements Serializable {

		@Column
		private String name;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "list")
		private Collection<BuildingListEntry> entries = new ArrayList<>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Collection<BuildingListEntry> getEntries() {
			return entries;
		}

		public void setEntries(Collection<BuildingListEntry> entries) {
			this.entries = entries;
		}
	}

	@Entity(name = "BuildingListEntry")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "TB_BUILDING_LIST_ENTRY")
	public static class BuildingListEntry extends DBObject implements Serializable {

		@Column
		protected String comments;

		@Column
		protected Integer priority;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "list_id")
		protected BuildingList list;

		public String getComments() {
			return comments;
		}

		public void setComments(String comments) {
			this.comments = comments;
		}

		public Integer getPriority() {
			return priority;
		}

		public void setPriority(Integer priority) {
			this.priority = priority;
		}

		public BuildingList getList() {
			return list;
		}

		public void setList(BuildingList list) {
			this.list = list;
		}
	}

	@Entity(name = "BLEHome")
	@Table(name = "TB_BLE_HOME")
	public static class BLEHome extends BuildingListEntry {

		@Column(name = "has_ctv")
		protected Integer hasCtv;

		public Integer getHasCtv() {
			return hasCtv;
		}

		public void setHasCtv(Integer hasCtv) {
			this.hasCtv = hasCtv;
		}
	}

	@Entity(name = "BLENonLiving")
	@Table(name = "TB_BLE_NONLIVING ")
	public static class BLENonLiving extends BuildingListEntry {

		@Column(name = "is_delayed")
		protected boolean delayed;

		public boolean isDelayed() {
			return delayed;
		}

		public void setDelayed(boolean delayed) {
			this.delayed = delayed;
		}
	}
}
