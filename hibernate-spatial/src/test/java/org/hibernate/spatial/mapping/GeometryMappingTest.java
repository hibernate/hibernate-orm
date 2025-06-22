/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.mapping;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spatial.GeolatteGeometryJavaType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.geolatte.geom.C2D;
import org.geolatte.geom.MultiLineString;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


@DomainModel(annotatedClasses = { GeometryMappingTest.MLEntity.class })
@ServiceRegistry
@SessionFactory
public class GeometryMappingTest {

	@Test
	public void testSimpleEntity(SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( MLEntity.class );

		ModelPart part = entityDescriptor.findSubPart( "lineString" );
		assertThat( part.getJavaType(), equalTo( GeolatteGeometryJavaType.MULTILINESTRING_INSTANCE ) );

	}

	@Entity(name = "MLEntity")
	public static class MLEntity {

		@Id
		private Integer id;
		private String type;
		private MultiLineString<C2D> lineString;

		public MLEntity() {
		}

		public MLEntity(Integer id, String type, MultiLineString<C2D> lineString) {
			this.id = id;
			this.type = type;
			this.lineString = lineString;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public MultiLineString<C2D> getLineString() {
			return lineString;
		}

		public void setLineString(MultiLineString<C2D> lineString) {
			this.lineString = lineString;
		}
	}
}
