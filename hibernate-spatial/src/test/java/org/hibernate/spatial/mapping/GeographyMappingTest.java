/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.mapping;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;
import org.geolatte.geom.codec.Wkt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


@DomainModel(annotatedClasses = { GeographyMappingTest.PointEntity.class })
@ServiceRegistry
@SessionFactory
public class GeographyMappingTest {

	@Test
	public void testSimpleEntity(SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( PointEntity.class );
		final JdbcTypeRegistry jdbcTypeRegistry = entityDescriptor.getFactory()
				.getTypeConfiguration()
				.getJdbcTypeRegistry();

		BasicValuedModelPart part = (BasicValuedModelPart) entityDescriptor.findSubPart( "location" );
		assertThat( part.getJdbcMapping().getJdbcType(), equalTo( jdbcTypeRegistry.getDescriptor( SqlTypes.GEOGRAPHY ) ) );
		scope.inTransaction(
				s -> {
					s.persist(
							new PointEntity(
									1,
									"test",
									(Point<G2D>) Wkt.fromWkt(
											"SRID=4326;POINT(48.2083736 16.3724441)"
									)
							)
					);
					s.flush();
					s.clear();

					PointEntity pointEntity = s.find( PointEntity.class, 1 );
					assertThat( pointEntity.location, is( notNullValue() ) );
				}
		);
	}

	@Entity(name = "MLEntity")
	public static class PointEntity {

		@Id
		private Integer id;
		private String type;
		@JdbcTypeCode(SqlTypes.GEOGRAPHY)
		private Point<G2D> location;

		public PointEntity() {
		}

		public PointEntity(Integer id, String type, Point<G2D> location) {
			this.id = id;
			this.type = type;
			this.location = location;
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

		public Point<G2D> getLineString() {
			return location;
		}

		public void setLineString(Point<G2D> location) {
			this.location = location;
		}
	}
}
