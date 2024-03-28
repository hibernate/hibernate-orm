/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity.E1;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity.E2;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.hibernate.testing.TestForIssue;
import org.hibernate.type.SqlTypes;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EnumSet extends BaseEnversJPAFunctionalTestCase {
	private Integer sse1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {EnumSetEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.PREFER_NATIVE_ENUM_TYPES, "false" );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EnumSetEntity sse1 = new EnumSetEntity();

		// Revision 1 (sse1: initialy 1 element)
		em.getTransaction().begin();

		sse1.getEnums1().add( E1.X );
		sse1.getEnums2().add( E2.A );

		em.persist( sse1 );

		em.getTransaction().commit();

		// Revision 2 (sse1: adding 1 element/removing a non-existing element)
		em.getTransaction().begin();

		sse1 = em.find( EnumSetEntity.class, sse1.getId() );

		sse1.getEnums1().add( E1.Y );
		sse1.getEnums2().remove( E2.B );

		em.getTransaction().commit();

		// Revision 3 (sse1: removing 1 element/adding an exisiting element)
		em.getTransaction().begin();

		sse1 = em.find( EnumSetEntity.class, sse1.getId() );

		sse1.getEnums1().remove( E1.X );
		sse1.getEnums2().add( E2.A );

		em.getTransaction().commit();

		//

		sse1_id = sse1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( EnumSetEntity.class, sse1_id ) );
	}

	@Test
	public void testHistoryOfSse1() {
		EnumSetEntity rev1 = getAuditReader().find( EnumSetEntity.class, sse1_id, 1 );
		EnumSetEntity rev2 = getAuditReader().find( EnumSetEntity.class, sse1_id, 2 );
		EnumSetEntity rev3 = getAuditReader().find( EnumSetEntity.class, sse1_id, 3 );

		assert rev1.getEnums1().equals( TestTools.makeSet( E1.X ) );
		assert rev2.getEnums1().equals( TestTools.makeSet( E1.X, E1.Y ) );
		assert rev3.getEnums1().equals( TestTools.makeSet( E1.Y ) );

		assert rev1.getEnums2().equals( TestTools.makeSet( E2.A ) );
		assert rev2.getEnums2().equals( TestTools.makeSet( E2.A ) );
		assert rev3.getEnums2().equals( TestTools.makeSet( E2.A ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7780")
	public void testEnumRepresentation() {
		EntityManager entityManager = getEntityManager();

		verifyModel( entityManager );

		{
			final String qry = "SELECT enums1 FROM EnumSetEntity_enums1_AUD ORDER BY REV ASC";
			List<String> enums1 = entityManager.createNativeQuery( qry, String.class ).getResultList();
			Assertions.assertThat( enums1 ).isEqualTo( List.of( "X", "Y", "X" ) );
		}

		{
			final String qry = "SELECT enums2 FROM EnumSetEntity_enums2_AUD ORDER BY REV ASC";
			String enum2 = (String) entityManager.createNativeQuery( qry, String.class ).getSingleResult();
			// Compare the String value to account for, as an example, Oracle returning a BigDecimal instead of an int.
			Assertions.assertThat( enum2 ).isEqualTo( "0" );
		}

		entityManager.close();
	}

	private void verifyModel(EntityManager entityManager) {
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
