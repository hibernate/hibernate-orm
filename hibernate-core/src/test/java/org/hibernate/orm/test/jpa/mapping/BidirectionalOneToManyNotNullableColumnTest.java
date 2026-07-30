/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.mapping;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.AnnotationException;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey(value = "HHH-13287")
public class BidirectionalOneToManyNotNullableColumnTest {

	@Test
	@ServiceRegistry
	public void test(ServiceRegistryScope scope) {
		final AnnotationException exception = assertThrows(
				AnnotationException.class,
				() -> MetadataBuildingTestHelper.buildMetadata( scope.getRegistry(), ParentData.class, ChildData.class )
		);
		assertThat( exception.getMessage() )
				.contains( "may not specify '@OrderColumn(nullable=false)'" );
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
