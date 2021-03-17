/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@TestForIssue(jiraKey = "HHH-13287")
public class BidirectionalOneToManyNotNullableColumnTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	@FailureExpected( jiraKey = "HHH-13287" )
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			ParentData parent = new ParentData();
			parent.setId( 1L );
			parent.addChildData( new ChildData() );
			parent.addChildData( new ChildData() );

			entityManager.persist( parent );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			ParentData parent = entityManager.find( ParentData.class, 1L );

			assertSame( 2, parent.getChildren().size() );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ParentData.class,
				ChildData.class
		};
	}

	@Entity(name = "ParentData")
	public static class ParentData {
		@Id
		long id;

		@OneToMany(mappedBy = "parentData", cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderColumn(name = "listOrder", nullable = false)
		private List<ChildData> children = new ArrayList<>();

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public List<ChildData> getChildren() {
			return children;
		}

		public void addChildData(ChildData childData) {
			childData.setParentData( this );
			children.add( childData );
		}
	}

	@Entity(name = "ChildData")
	public static class ChildData {
		@Id
		@GeneratedValue
		long id;

		@ManyToOne
		private ParentData parentData;

		public ChildData() {
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public ParentData getParentData() {
			return parentData;
		}

		public void setParentData(ParentData parentData) {
			this.parentData = parentData;
		}
	}

}
