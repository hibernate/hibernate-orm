/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.sql.Types;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey( "HHH-13393" )
@DomainModel( annotatedClasses = ZoneMappingTests.ZoneMappingTestEntity.class )
@SessionFactory
public class ZoneMappingTests {

	@Test
	public void basicAssertions(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().getEntityDescriptor( ZoneMappingTestEntity.class );
		final JdbcTypeRegistry jdbcTypeRegistry = sessionFactory.getTypeConfiguration().getJdbcTypeRegistry();

		{
			final BasicAttributeMapping zoneIdAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "zoneId" );
			assertThat( zoneIdAttribute.getJdbcMapping().getJdbcType() ).isEqualTo( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ) );
			assertThat( zoneIdAttribute.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( ZoneId.class );
		}

		{
			final BasicAttributeMapping zoneOffsetAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "zoneOffset" );
			assertThat( zoneOffsetAttribute.getJdbcMapping().getJdbcType() ).isEqualTo( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ) );
			assertThat( zoneOffsetAttribute.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( ZoneOffset.class );
		}
	}

	@Test
	public void testUsage(SessionFactoryScope scope) {
		final ZoneMappingTestEntity entity = new ZoneMappingTestEntity( 1, "one", ZoneId.systemDefault(), ZoneOffset.UTC );
		final ZoneMappingTestEntity entity2 = new ZoneMappingTestEntity( 2, "two", ZoneId.systemDefault(), ZoneOffset.ofHours( 0 ) );
		final ZoneMappingTestEntity entity3 = new ZoneMappingTestEntity( 3, "three", ZoneId.systemDefault(), ZoneOffset.ofHours( -10 ) );

		scope.inTransaction( (session) -> {
			session.persist( entity );
			session.persist( entity2 );
			session.persist( entity3 );
		} );

		scope.inTransaction( (session) -> session.createQuery( "from ZoneMappingTestEntity" ).list() );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17726" )
	public void testUpdateQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new ZoneMappingTestEntity(
				1,
				"one",
				ZoneId.systemDefault(),
				ZoneOffset.MIN
		) ) );
		scope.inTransaction( session -> {
			final ZoneId zoneId = ZoneId.of( "UTC" );
			final ZoneOffset zoneOffset = ZoneOffset.from( ZoneOffset.UTC );
			session.createMutationQuery(
					"update ZoneMappingTestEntity e set e.zoneId = :zoneId, e.zoneOffset = :zoneOffset"
			).setParameter( "zoneId", zoneId ).setParameter( "zoneOffset", zoneOffset ).executeUpdate();
			final ZoneMappingTestEntity entity = session.find( ZoneMappingTestEntity.class, 1 );
			assertThat( entity.getZoneId() ).isEqualTo( zoneId );
			assertThat( entity.getZoneOffset() ).isEqualTo( zoneOffset );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "ZoneMappingTestEntity" )
	@Table( name = "zone_map_test_entity" )
	public static class ZoneMappingTestEntity {
		private Integer id;

		private String name;

		private ZoneId zoneId;
		private Set<ZoneId> zoneIds = new HashSet<>();

		private ZoneOffset zoneOffset;
		private Set<ZoneOffset> zoneOffsets = new HashSet<>();

		public ZoneMappingTestEntity() {
		}

		public ZoneMappingTestEntity(Integer id, String name, ZoneId zoneId, ZoneOffset zoneOffset) {
			this.id = id;
			this.name = name;
			this.zoneId = zoneId;
			this.zoneOffset = zoneOffset;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ZoneId getZoneId() {
			return zoneId;
		}

		public void setZoneId(ZoneId zoneId) {
			this.zoneId = zoneId;
		}

		@ElementCollection
		@CollectionTable( name = "zone_ids", joinColumns = @JoinColumn( name = "entity_id" ) )
		@Column( name = "zone_id" )
		public Set<ZoneId> getZoneIds() {
			return zoneIds;
		}

		public void setZoneIds(Set<ZoneId> zoneIds) {
			this.zoneIds = zoneIds;
		}

		public ZoneOffset getZoneOffset() {
			return zoneOffset;
		}

		public void setZoneOffset(ZoneOffset zoneOffset) {
			this.zoneOffset = zoneOffset;
		}

		@ElementCollection
		@CollectionTable( name = "zone_offsets", joinColumns = @JoinColumn( name = "entity_id" ) )
		@Column( name = "zone_offset" )
		public Set<ZoneOffset> getZoneOffsets() {
			return zoneOffsets;
		}

		public void setZoneOffsets(Set<ZoneOffset> zoneOffsets) {
			this.zoneOffsets = zoneOffsets;
		}
	}
}
