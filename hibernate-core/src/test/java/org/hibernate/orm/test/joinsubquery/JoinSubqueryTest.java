/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.joinsubquery;

import jakarta.persistence.*;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(annotatedClasses = {
		JoinSubqueryTest.RecordItem.class,
		JoinSubqueryTest.RecordType.class
})
@SessionFactory
@JiraKey("HHH-19052")
class JoinSubqueryTest {

	@BeforeAll
	static void setUp(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(session -> {
			final var id = 1L;
			final var typeId = 42L;
			final var recordType = new RecordType(id, typeId);
			session.persist(recordType);
			final var item = new RecordItem(id, typeId, recordType);
			session.persist(item);
		});
	}

	@Test
	void test(SessionFactoryScope scope) throws Exception {
		scope.inSession(session -> {
			final var item = session.get(RecordItem.class, 1L);
			assertNotNull(item);
		});
	}

	@Entity
	@Table(name = "record_items")
	public static class RecordItem implements Serializable {

		@Id
		protected Long id;

		@Column(name = "type_id", insertable = false, updatable = false)
		private Long typeId;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumnOrFormula(column = @JoinColumn(name = "type_id", referencedColumnName = "entity_id"))
		@JoinColumnOrFormula(formula = @JoinFormula(value = "(SELECT x.id FROM record_types x WHERE x.entity_id = type_id)", referencedColumnName = "id"))
		private RecordType type;

		RecordItem() {
		}

		public RecordItem(Long id, Long typeId, RecordType type) {
			this.id = id;
			this.typeId = typeId;
			this.type = type;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return this.id;
		}

		public Long getTypeId() {
			return typeId;
		}

		public RecordType getType() {
			return type;
		}


	}

	@Entity
	@Table(name = "record_types")
	public static class RecordType implements Serializable {

		@Id
		protected Long id;

		@Column(name = "entity_id")
		private Long entityId;

		RecordType() {
		}

		public RecordType(Long id, Long entityId) {
			this.id = id;
			this.entityId = entityId;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return this.id;
		}

		public Long getEntityId() {
			return entityId;
		}

		public void setEntityId(Long entityId) {
			this.entityId = entityId;
		}

	}
}
