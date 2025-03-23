/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@JiraKey(value = "HHH-15418")
@DomainModel(
		annotatedClasses = {
				CompositeKeyNativeQueryTest.Zone.class
		}
)
@SessionFactory
public class CompositeKeyNativeQueryTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					short id = 1;
					ZoneId zoneId = new ZoneId( id, "1" );
					Zone zone = new Zone( zoneId );
					session.persist( zone );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String sql = "SELECT {c.*} FROM ZONE_TABLE c WHERE c.id1 = 1";
					List<Zone> zones = session.createNativeQuery( sql, Zone.class, "c" ).list();
				}
		);
	}

	@Entity(name = "Zone")
	@Table(name = "ZONE_TABLE")
	public static class Zone {
		@EmbeddedId
		private ZoneId pk;

		private String zondescrip;

		public Zone() {
		}

		public Zone(ZoneId pk) {
			this.pk = pk;
		}

		public ZoneId getPk() {
			return pk;
		}

		public void setPk(ZoneId pk) {
			this.pk = pk;
		}

	}

	@Embeddable
	public static class ZoneId implements Serializable {
		private Short id1;
		private String id2;

		public ZoneId() {
		}

		public ZoneId(Short id1, String id2) {
			this.id1 = id1;
			this.id2 = id2;
		}

		public Short getId1() {
			return id1;
		}

		public void setId1(Short id1) {
			this.id1 = id1;
		}

		public String getId2() {
			return id2;
		}

		public void setId2(String id2) {
			this.id2 = id2;
		}
	}
}
