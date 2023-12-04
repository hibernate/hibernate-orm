/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.id;

import java.util.Set;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.categorize.spi.AggregatedKeyMapping;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.NonAggregatedKeyMapping;
import org.hibernate.cache.spi.access.AccessType;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.buildHierarchyMetadata;

/**
 * @author Steve Ebersole
 */
public class SimpleIdTests {
	@Test
	void testSimpleId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( BasicIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final BasicKeyMapping idMapping = (BasicKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNotNull();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		final BasicKeyMapping naturalIdMapping = (BasicKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( NaturalId.class ) ).isNotNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
	}

	@Test
	void testAggregatedId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( AggregatedIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final AggregatedKeyMapping idMapping = (AggregatedKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNull();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNotNull();

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		assertThat( entityHierarchy.getNaturalIdMapping() ).isInstanceOf( AggregatedKeyMapping.class );
		final AggregatedKeyMapping naturalIdMapping = (AggregatedKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( NaturalId.class ) ).isNotNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
	}

	@Test
	void testNonAggregatedId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( NonAggregatedIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final NonAggregatedKeyMapping idMapping = (NonAggregatedKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getIdAttributes() ).hasSize( 2 );
		assertThat( idMapping.getIdAttributes().stream().map( AttributeMetadata::getName ) ).containsExactly( "id1", "id2" );
		assertThat( idMapping.getIdClassType().getClassName() ).isEqualTo( NonAggregatedIdEntity.Pk.class.getName() );

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		final NonAggregatedKeyMapping naturalIdMapping = (NonAggregatedKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getIdAttributes() ).hasSize( 2 );
		assertThat( naturalIdMapping.getIdAttributes().stream().map( AttributeMetadata::getName ) ).containsExactly( "naturalKey1", "naturalKey2" );

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();

		assertThat( entityHierarchy.getCacheRegion() ).isNotNull();
		assertThat( entityHierarchy.getCacheRegion().getAccessType() ).isEqualTo( AccessType.TRANSACTIONAL );

		assertThat( entityHierarchy.getInheritanceType() ).isNotNull();
		assertThat( entityHierarchy.getInheritanceType() ).isEqualTo( InheritanceType.TABLE_PER_CLASS );
	}
}
