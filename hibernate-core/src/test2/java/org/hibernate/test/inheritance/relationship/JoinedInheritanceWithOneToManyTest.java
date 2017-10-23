/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.relationship;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.annotations.inheritance.singletable.Building;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JoinedInheritanceWithOneToManyTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			BuildingList.class,
			BuildingListEntry.class,
			BLEHome.class,
			BLENonLiving.class,
		};
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
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
		doInHibernate( this::sessionFactory, session -> {
			List<BuildingList> buildingLists = session.createQuery( "from BuildingList" ).getResultList();
			BuildingList buildingList = buildingLists.get( 0 );
			assertEquals(2, buildingList.getEntries().size());
		});
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
	@SequenceGenerator(name = "seq",
			sequenceName = "sq_building_list_entry_id",
			allocationSize = 1)
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

		protected boolean delayed;

		public boolean isDelayed() {
			return delayed;
		}

		public void setDelayed(boolean delayed) {
			this.delayed = delayed;
		}
	}
}

