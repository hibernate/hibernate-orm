/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-16358")
@DomainModel(
		annotatedClasses = {
				ManyToManyAbstractTablePerClassTest.TablePerClassBase.class,
				ManyToManyAbstractTablePerClassTest.TablePerClassSub1.class,
				ManyToManyAbstractTablePerClassTest.TablePerClassSub2.class
		}
)
@SessionFactory
public class ManyToManyAbstractTablePerClassTest {

	@Test
	public void testAddAndRemove(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TablePerClassSub1 o1 = session.find( TablePerClassSub1.class, 1 );
			assertNotNull( o1 );
			assertEquals( 1, o1.childrenSet.size() );
			assertEquals( 1, o1.childrenList.size() );
			assertEquals( 1, o1.childrenMap.size() );
			TablePerClassBase o2 = o1.childrenSet.iterator().next();
			assertEquals( 2, o2.id );
			assertEquals( 2, o1.childrenList.get( 0 ).id );
			assertEquals( 2, o1.childrenMap.get( 2 ).id );
			o1.childrenSet.remove( o2 );
			o1.childrenList.remove( 0 );
			o1.childrenMap.remove( 2 );
			TablePerClassSub1 o3 = new TablePerClassSub1( 3 );
			session.persist( o3 );
			o1.childrenSet.add( o3 );
			o1.childrenList.add( o3 );
			o1.childrenMap.put( 3, o3 );
			session.flush();
		} );
		scope.inTransaction( session -> {
			final TablePerClassSub1 o1 = session.find( TablePerClassSub1.class, 1 );
			assertNotNull( o1 );
			assertEquals( 1, o1.childrenSet.size() );
			assertEquals( 1, o1.childrenList.size() );
			assertEquals( 1, o1.childrenMap.size() );
			TablePerClassBase o2 = o1.childrenSet.iterator().next();
			assertEquals( 3, o2.id );
			assertEquals( 3, o1.childrenList.get( 0 ).id );
			assertEquals( 3, o1.childrenMap.get( 3 ).id );
		} );
	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TablePerClassSub1 o1 = new TablePerClassSub1( 1 );
			TablePerClassSub2 o2 = new TablePerClassSub2( 2 );
			o1.childrenSet.add( o2 );
			o1.childrenList.add( o2 );

			session.persist( o2 );
			session.persist( o1 );

			o1.childrenMap.put( 2, o2 );
		} );
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity(name = "TablePerClassBase")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class TablePerClassBase {
		@Id
		Integer id;
		@ManyToMany
		@JoinTable(name = "children_set")
		Set<TablePerClassBase> childrenSet = new HashSet<>();
		@ManyToMany
		@JoinTable(name = "children_list")
		@OrderColumn(name = "listIndex")
		List<TablePerClassBase> childrenList = new ArrayList<>();
		@ManyToMany
		@JoinTable(name = "children_map")
		Map<Integer, TablePerClassBase> childrenMap = new HashMap<>();

		public TablePerClassBase() {
		}

		public TablePerClassBase(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "TablePerClassSub1")
	@Table(name = "table_per_class_sub_1")
	public static class TablePerClassSub1 extends TablePerClassBase {
		public TablePerClassSub1() {
		}

		public TablePerClassSub1(Integer id) {
			super( id );
		}
	}

	@Entity(name = "TablePerClassSub2")
	@Table(name = "table_per_class_sub_2")
	public static class TablePerClassSub2 extends TablePerClassBase {
		public TablePerClassSub2() {
		}

		public TablePerClassSub2(Integer id) {
			super( id );
		}
	}
}
