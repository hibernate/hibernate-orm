/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated.mappedSuperclass;

import java.io.Serializable;
import java.sql.Types;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static javax.persistence.EnumType.STRING;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * Originally developed to verify/diagnose HHH-10128
 *
 * @author Steve Ebersole
 */
public class EnumeratedWithMappedSuperclassTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void before() {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PostgreSQL81Dialect.class )
				.build();
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testHHH10128() {
		final Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( Entity.class )
				.addAnnotatedClass( DescriptionEntity.class )
				.addAnnotatedClass( AddressLevel.class )
				.buildMetadata();

		final PersistentClass addressLevelBinding = metadata.getEntityBinding( AddressLevel.class.getName() );

		final Property natureProperty = addressLevelBinding.getProperty( "nature" );
		CustomType customType = assertTyping( CustomType.class, natureProperty.getType() );
		EnumType enumType = assertTyping( EnumType.class, customType.getUserType() );
		assertEquals( Types.VARCHAR, enumType.sqlTypes()[0] );

		SessionFactoryImplementor sf = (SessionFactoryImplementor) metadata.buildSessionFactory();
		try {
			EntityPersister p = sf.getEntityPersister( AddressLevel.class.getName() );
			CustomType runtimeType = assertTyping( CustomType.class, p.getPropertyType( "nature" ) );
			EnumType runtimeEnumType = assertTyping( EnumType.class, runtimeType.getUserType() );
			assertEquals( Types.VARCHAR, runtimeEnumType.sqlTypes()[0] );
		}
		finally {
			sf.close();
		}
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

	@javax.persistence.Entity(name = "AddressLevel")
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
