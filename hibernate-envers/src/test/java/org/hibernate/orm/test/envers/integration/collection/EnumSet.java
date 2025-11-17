/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity.E1;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity.E2;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {EnumSetEntity.class},
		integrationSettings = @Setting(name = AvailableSettings.PREFER_NATIVE_ENUM_TYPES, value = "false"))
public class EnumSet {
	private Integer sse1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (sse1: initialy 1 element)
		scope.inTransaction( em -> {
			EnumSetEntity sse1 = new EnumSetEntity();
			sse1.getEnums1().add( E1.X );
			sse1.getEnums2().add( E2.A );
			em.persist( sse1 );
			sse1_id = sse1.getId();
		} );

		// Revision 2 (sse1: adding 1 element/removing a non-existing element)
		scope.inTransaction( em -> {
			EnumSetEntity sse1 = em.find( EnumSetEntity.class, sse1_id );
			sse1.getEnums1().add( E1.Y );
			sse1.getEnums2().remove( E2.B );
		} );

		// Revision 3 (sse1: removing 1 element/adding an exisiting element)
		scope.inTransaction( em -> {
			EnumSetEntity sse1 = em.find( EnumSetEntity.class, sse1_id );
			sse1.getEnums1().remove( E1.X );
			sse1.getEnums2().add( E2.A );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( EnumSetEntity.class, sse1_id ) );
		} );
	}

	@Test
	public void testHistoryOfSse1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EnumSetEntity rev1 = auditReader.find( EnumSetEntity.class, sse1_id, 1 );
			EnumSetEntity rev2 = auditReader.find( EnumSetEntity.class, sse1_id, 2 );
			EnumSetEntity rev3 = auditReader.find( EnumSetEntity.class, sse1_id, 3 );

			assertEquals( TestTools.makeSet( E1.X ), rev1.getEnums1() );
			assertEquals( TestTools.makeSet( E1.X, E1.Y ), rev2.getEnums1() );
			assertEquals( TestTools.makeSet( E1.Y ), rev3.getEnums1() );

			assertEquals( TestTools.makeSet( E2.A ), rev1.getEnums2() );
			assertEquals( TestTools.makeSet( E2.A ), rev2.getEnums2() );
			assertEquals( TestTools.makeSet( E2.A ), rev3.getEnums2() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7780")
	public void testEnumRepresentation(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			verifyModel( em );

			{
				final String qry = "SELECT enums1 FROM EnumSetEntity_enums1_AUD ORDER BY REV ASC";
				List<String> enums1 = em.createNativeQuery( qry, String.class ).getResultList();
				Assertions.assertThat( enums1 ).isEqualTo( List.of( "X", "Y", "X" ) );
			}

			{
				final String qry = "SELECT enums2 FROM EnumSetEntity_enums2_AUD ORDER BY REV ASC";
				String enum2 = (String) em.createNativeQuery( qry, String.class ).getSingleResult();
				// Compare the String value to account for, as an example, Oracle returning a BigDecimal instead of an int.
				Assertions.assertThat( enum2 ).isEqualTo( "0" );
			}
		} );
	}

	private void verifyModel(jakarta.persistence.EntityManager entityManager) {
		final MappingMetamodelImplementor mappingMetamodel = entityManager.unwrap( SessionImplementor.class )
				.getFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();

		{
			final EntityMappingType entityMapping = mappingMetamodel.getEntityDescriptor( EnumSetEntity.class );
			final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) entityMapping.findDeclaredAttributeMapping( "enums1" );
			verifyMapping( attributeMapping.getElementDescriptor().getJdbcMapping( 0 ) );
		}

		{
			final EntityMappingType entityMapping = mappingMetamodel.getEntityDescriptor( "EnumSetEntity_enums1_AUD" );
			final CompositeIdentifierMapping cidMapping = (CompositeIdentifierMapping) entityMapping.getIdentifierMapping();
			verifyMapping( cidMapping.getEmbeddableTypeDescriptor().findAttributeMapping( "element" ).getJdbcMapping( 0 ) );
		}
	}

	private void verifyMapping(JdbcMapping jdbcMapping) {
		assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode() ).isIn( Types.VARCHAR, Types.NVARCHAR, SqlTypes.ENUM );
	}
}
