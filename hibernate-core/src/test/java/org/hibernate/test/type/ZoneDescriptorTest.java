/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type;

import java.sql.Types;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test support for mapping {@link java.time.ZoneId} and {@link java.time.ZoneOffset} values
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-13393" )
public class ZoneDescriptorTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void basicAssertions() {
		final EntityPersister entityDescriptor = sessionFactory().getMetamodel().entityPersister( ZoneMappingTestEntity.class );

		{
			final Type zoneIdAttrDescriptor = entityDescriptor.getPropertyType( "zoneId" );
			final int[] sqlTypes = zoneIdAttrDescriptor.sqlTypes( sessionFactory() );
			assertThat( sqlTypes.length, is( 1 ) );
			assertThat( sqlTypes[0], CoreMatchers.anyOf( is( Types.VARCHAR ), is( Types.NVARCHAR ) ) );
		}

		{
			final Type zoneOffsetAttrDescriptor = entityDescriptor.getPropertyType( "zoneOffset" );
			final int[] sqlTypes = zoneOffsetAttrDescriptor.sqlTypes( sessionFactory() );
			assertThat( sqlTypes.length, is( 1 ) );
			assertThat( sqlTypes[0], CoreMatchers.anyOf( is( Types.INTEGER ), is( Types.NVARCHAR ) ) );
		}
	}

	@Test
	public void testUsage() {
		final ZoneMappingTestEntity entity = new ZoneMappingTestEntity( 1, "one", ZoneId.systemDefault(), ZoneOffset.UTC );
		inTransaction( session -> session.save( entity ) );

		try {
			inTransaction( session -> session.createQuery( "from ZoneMappingTestEntity" ).list() );
		}
		finally {
			inTransaction( session -> session.delete( entity ) );
		}
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( ZoneMappingTestEntity.class );
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
