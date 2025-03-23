/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.enumerated.mappedSuperclass;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static jakarta.persistence.EnumType.STRING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Originally developed to verify/diagnose HHH-10128
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@DomainModel(annotatedClasses = {
		EnumeratedWithMappedSuperclassTest.Entity.class,
		EnumeratedWithMappedSuperclassTest.DescriptionEntity.class,
		EnumeratedWithMappedSuperclassTest.AddressLevel.class
} )
@SessionFactory
@RequiresDialect( value = PostgreSQLDialect.class, majorVersion = 8, minorVersion = 1 )
public class EnumeratedWithMappedSuperclassTest {

	@Test
	public void testHHH10128(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		// check the boot model
		final MetadataImplementor domainModel = modelScope.getDomainModel();
		final PersistentClass addressLevelBinding = domainModel.getEntityBinding( AddressLevel.class.getName() );
		final Property natureProperty = addressLevelBinding.getProperty( "nature" );
		//noinspection unchecked
		BasicType<Nature> natureMapping = (BasicType<Nature>) natureProperty.getType();
		assertThat( natureMapping.getJdbcType().getJdbcTypeCode() )
				.isIn( SqlTypes.VARCHAR, SqlTypes.ENUM, SqlTypes.NAMED_ENUM );

		// check the runtime model
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( AddressLevel.class.getName() );
		final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) persister.findAttributeMapping( "nature" );
		final JdbcType jdbcType = attributeMapping.getJdbcMapping().getJdbcType();
		assertThat( jdbcType.getJdbcTypeCode() )
				.isIn( SqlTypes.VARCHAR, SqlTypes.ENUM, SqlTypes.NAMED_ENUM );
	}

	@MappedSuperclass
	public static abstract class Entity implements Serializable {
		public static final String PROPERTY_NAME_ID = "id";
		@Id
		@GeneratedValue(generator = "uuid2")
		@GenericGenerator(name = "uuid2", strategy = "uuid2")
		@Column(columnDefinition = "varchar", unique = true, nullable = false)
		private String id;

		public String getId() {
			return id;
		}

		public void setId(final String id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public static abstract class DescriptionEntity extends Entity {
		@Column(name = "description_lang1", nullable = false, length = 100)
		private String descriptionLang1;
		@Column(name = "description_lang2", length = 100)
		private String descriptionLang2;
		@Column(name = "description_lang3", length = 100)
		private String descriptionLang3;

		public String getDescriptionLang1() {
			return this.descriptionLang1;
		}

		public void setDescriptionLang1(final String descriptionLang1) {
			this.descriptionLang1 = descriptionLang1;
		}

		public String getDescriptionLang2() {
			return this.descriptionLang2;
		}

		public void setDescriptionLang2(final String descriptionLang2) {
			this.descriptionLang2 = descriptionLang2;
		}

		public String getDescriptionLang3() {
			return this.descriptionLang3;
		}

		public void setDescriptionLang3(final String descriptionLang3) {
			this.descriptionLang3 = descriptionLang3;
		}
	}

	public static enum Nature {
		LIST, EXLIST, INPUT
	}

	@jakarta.persistence.Entity(name = "AddressLevel")
	@Table(name = "address_level")
	public static class AddressLevel extends DescriptionEntity {
//		@Column(columnDefinition = "varchar", nullable = false, length = 100)
		@Enumerated(STRING)
		private Nature nature;
		@Column(nullable = false)
		private Integer rank;
		@Column(nullable = false)
		private boolean required;

		public AddressLevel() { // Do nothing, default constructor needed by JPA / Hibernate
		}

		public Nature getNature() {
			return this.nature;
		}

		public void setNature(final Nature nature) {
			this.nature = nature;
		}

		public Integer getRank() {
			return this.rank;
		}

		public void setRank(final Integer rank) {
			this.rank = rank;
		}

		public boolean getRequired() {
			return this.required;
		}

		public void isRequired(final boolean required) {
			this.required = required;
		}
	}

}
