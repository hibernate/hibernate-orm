/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mapsid;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(
		annotatedClasses = {MapsEmbeddedIdNamingTest.BaseEntity.class, MapsEmbeddedIdNamingTest.BaseExtensionEntity.class, MapsEmbeddedIdNamingTest.ReferringEntity.class})
@Jira("https://hibernate.atlassian.net/browse/HHH-9407")
public class MapsEmbeddedIdNamingTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			BaseEntity base = new BaseEntity();
			base.id = 1;
			session.persist( base );

			BaseExtensionEntity extensionEntity = new BaseExtensionEntity();
			extensionEntity.id = new BaseExtensionId( base.id, 1 );
			extensionEntity.base = base;
			session.persist( extensionEntity );

			ReferringEntity referringEntity = new ReferringEntity();
			referringEntity.id = 1;
			referringEntity.extension = extensionEntity;
			session.persist( referringEntity );
		} );
	}

	@Entity
	public static class BaseEntity {
		@Id
		@Column(name = "base_id")
		public Integer id;
	}

	@Entity
	public static class BaseExtensionEntity {
		@EmbeddedId
		public BaseExtensionId id;
		@ManyToOne(optional = false)
		@MapsId("baseId")
		public BaseEntity base;
	}

	@Embeddable
	public record BaseExtensionId(
			@Column(name = "base_id")
			Integer baseId,
			@Column(name = "ext_number")
			int extensionNumber) {
	}

	@Entity
	public static class ReferringEntity {
		@Id
		Integer id;

		@ManyToOne(optional = false)
		@JoinColumns({
				@JoinColumn(name = "base_id", referencedColumnName = "base_id", insertable = false, updatable = false),
				@JoinColumn(name = "ext_number", referencedColumnName = "ext_number", insertable = false,
						updatable = false)
		})
		public BaseExtensionEntity extension;
	}

}
