/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.model.runtime.basic.sql;

import java.sql.Types;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.SqlType;
import org.hibernate.annotations.SqlTypeDescriptor;
import org.hibernate.annotations.SqlTypeRegistration;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeBasic;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

import org.hibernate.testing.junit5.DatabaseAgnostic;
import org.hibernate.testing.orm.junit.FailureExpectedExtension;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.Assert.assertEquals;

/**
 * todo (6.0) : create a MetamodelBasedTesting (could be first use of `@DomainModel`!)
 *
 * @author Steve Ebersole
 */
@DatabaseAgnostic
@ExtendWith( FailureExpectedExtension.class )
@SuppressWarnings("WeakerAccess")
public class SqlTypeInfluencingTests {
	@Test
	public void testExplicitSqlType() {
		final Metadata metadata = new MetadataSources()
				.addAnnotatedClass( SqlTypeEntity.class )
				.buildMetadata();

		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

		final EntityTypeDescriptor entityDescriptor = sessionFactory.getMetamodel().entity( SqlTypeEntity.class );
		final SingularPersistentAttributeBasic nameAttribute = (SingularPersistentAttributeBasic) entityDescriptor.getAttribute( "name" );
		assertEquals(
				nameAttribute.getValueMapper().getSqlExpressableType().getSqlTypeDescriptor().getJdbcTypeCode(),
				Types.NCLOB
		);
	}

	@Test
	public void testExplicitSqlTypeDescriptor() {
		final Metadata metadata = new MetadataSources()
				.addAnnotatedClass( SqlTypeDescriptorEntity.class )
				.buildMetadata();

		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

		final EntityTypeDescriptor entityDescriptor = sessionFactory.getMetamodel().entity( SqlTypeDescriptorEntity.class );
		final SingularPersistentAttributeBasic nameAttribute = (SingularPersistentAttributeBasic) entityDescriptor.getAttribute( "name" );
		assertEquals(
				nameAttribute.getValueMapper().getSqlExpressableType().getSqlTypeDescriptor().getClass(),
				CustomStringSqlDescriptor.class
		);

	}

	@Test
	@FailureExpected( "@SqlTypeRegistration support not yet implemented" )
	public void testSqlTypeRegistrationOverride() {
		final Metadata metadata = new MetadataSources()
				.addAnnotatedClass( SqlTypeRegistrationOverrideEntity.class )
				.buildMetadata();

		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

		final EntityTypeDescriptor entityDescriptor = sessionFactory.getMetamodel().entity( SqlTypeRegistrationOverrideEntity.class );
		final SingularPersistentAttributeBasic nameAttribute = (SingularPersistentAttributeBasic) entityDescriptor.getAttribute( "name" );
		assertEquals(
				nameAttribute.getValueMapper().getSqlExpressableType().getSqlTypeDescriptor().getClass(),
				CustomStringSqlDescriptor.class
		);

	}

	@Test
	@FailureExpected( "@SqlTypeRegistration support not yet implemented" )
	public void testSqlTypeRegistrationExtension() {
		final Metadata metadata = new MetadataSources()
				.addAnnotatedClass( SqlTypeRegistrationExtensionEntity.class )
				.buildMetadata();

		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

		final EntityTypeDescriptor entityDescriptor = sessionFactory.getMetamodel().entity( SqlTypeRegistrationExtensionEntity.class );
		final SingularPersistentAttributeBasic nameAttribute = (SingularPersistentAttributeBasic) entityDescriptor.getAttribute( "name" );
		assertEquals(
				nameAttribute.getValueMapper().getSqlExpressableType().getSqlTypeDescriptor().getClass(),
				CustomCodeStringSqlDescriptor.class
		);

	}

	@Entity( name = "SqlTypeEntity" )
	@Table( name = "SqlTypeEntity" )
	public static class SqlTypeEntity {
		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@SqlType( Types.NCLOB )
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "SqlTypeDescriptorEntity" )
	@Table( name = "SqlTypeDescriptorEntity" )
	public static class SqlTypeDescriptorEntity {
		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@SqlTypeDescriptor( CustomStringSqlDescriptor.class )
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	/**
	 * The SqlTypeRegistration overrides the standard VARCHAR descriptor with our custom one
	 */
	@Entity( name = "SqlTypeRegistrationEntity" )
	@Table( name = "SqlTypeRegistrationEntity" )
	@SqlTypeRegistration( CustomStringSqlDescriptor.class )
	public static class SqlTypeRegistrationOverrideEntity {
		private Integer id;
		private String name;

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
	}

	/**
	 * The SqlTypeRegistration overrides the standard VARCHAR descriptor with our custom one
	 */
	@Entity( name = "SqlTypeRegistrationEntity" )
	@Table( name = "SqlTypeRegistrationEntity" )
	@SqlTypeRegistration( CustomCodeStringSqlDescriptor.class )
	public static class SqlTypeRegistrationExtensionEntity {
		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@SqlType( CustomCodeStringSqlDescriptor.TYPE_CODE )
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class CustomStringSqlDescriptor extends VarcharSqlDescriptor {
	}

	public static class CustomCodeStringSqlDescriptor extends VarcharSqlDescriptor {
		public static final int TYPE_CODE = Integer.MAX_VALUE;

		@Override
		public int getJdbcTypeCode() {
			return TYPE_CODE;
		}
	}
}
