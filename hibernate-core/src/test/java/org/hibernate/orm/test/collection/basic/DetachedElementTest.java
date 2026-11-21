package org.hibernate.orm.test.collection.basic;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = {DetachedElementTest.EntityWithList.class, DetachedElementTest.ListElement.class,
				DetachedElementTest.EntityWithMap.class, DetachedElementTest.MapElement.class}
)
@SessionFactory
@RequiresDialect( H2Dialect.class )
public class DetachedElementTest {

	@Test
	public void testList(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.getTransaction().begin();
					EntityWithList parent = new EntityWithList(1);
					ListElement element = new ListElement(2, parent);
					session.persist( parent );
					session.persist( element );
					session.getTransaction().commit();
					session.clear();

					// shouldn't throw exceptions for these operations
					assertDoesNotThrow(
							() -> {
								assertEquals( 1, parent.children.size() );
								assertFalse( parent.children.isEmpty() );
								assertTrue( parent.children.contains(element) );
								// the collection is initialized, so these read its state in memory
								assertEquals( 1, Hibernate.size(parent.children) );
								assertFalse( Hibernate.isEmpty(parent.children) );
								assertTrue( Hibernate.contains(parent.children, element) );
								assertSame( element, Hibernate.get(parent.children, 0) );
								assertTrue( parent.children.remove(element) );
							}
					);

					// an uninitialized collection of a detached entity can't be read
					final EntityWithList loaded = session.find( EntityWithList.class, 1L );
					session.clear();
					assertFalse( Hibernate.isInitialized(loaded.children) );
					assertThrows( LazyInitializationException.class,
							() -> Hibernate.get(loaded.children, 0)
					);
				}
		);
	}

	@Test
	void testMap(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
				session.getTransaction().begin();
				EntityWithMap parent = new EntityWithMap(1);
				MapElement element = new MapElement(2, parent);
				session.persist(parent);
				session.persist(element);
				session.getTransaction().commit();
				session.clear();

					// shouldn't throw exceptions for these operations
					assertDoesNotThrow(
							() -> {
								assertEquals( 1, parent.children.size() );
								assertFalse( parent.children.isEmpty() );
								assertTrue( parent.children.containsKey(element.id) );
								assertTrue( parent.children.containsValue(element) );
								// the map is initialized, so this reads its state in memory
								assertSame( element, Hibernate.get(parent.children, 2L) );
							}
					);

					// an uninitialized map of a detached entity can't be read
					final EntityWithMap loaded = session.find( EntityWithMap.class, 1L );
					session.clear();
					assertFalse( Hibernate.isInitialized(loaded.children) );
					assertThrows( LazyInitializationException.class,
							() -> Hibernate.get(loaded.children, 2L)
					);
			}
		);
	}

	@Entity
	public static class EntityWithList {
		@Id
		long id;

		@OneToMany(mappedBy = "parent")
		List<ListElement> children = new ArrayList<>();

		public EntityWithList(int id) {
			this.id = id;
		}

		protected EntityWithList() {}
	}

	@Entity
	public static class ListElement {
		@Id
		long id;

		@ManyToOne
		private EntityWithList parent;

		public ListElement(long id, EntityWithList parent) {
			this.id = id;
			this.parent = parent;
			parent.children.add(this);
		}

		protected ListElement() {}
	}

	@Entity
	public static class EntityWithMap {
		@Id
		long id;

		@OneToMany(mappedBy = "parent")
		Map<Long, MapElement> children = new HashMap<>();

		public EntityWithMap(int id) {
			this.id = id;
		}

		protected EntityWithMap() {}
	}

	@Entity
	public static class MapElement {
		@Id
		long id;

		@ManyToOne
		private EntityWithMap parent;

		public MapElement(long id, EntityWithMap parent) {
			this.id = id;
			this.parent = parent;
			parent.children.put(id, this);
		}

		protected MapElement() {}
	}

}
