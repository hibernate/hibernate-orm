/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.BasicValuedSingularAttributeMapping;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		annotatedClasses = SmokeTests.SimpleEntity.class
)
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
public class SmokeTests {

	@Test
	public void testSimpleEntity(SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory()
				.getDomainModel()
				.getEntityDescriptor( SimpleEntity.class );

		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		assert Integer.class.equals( identifierMapping.getMappedTypeDescriptor().getMappedJavaTypeDescriptor().getJavaType() );

		{
			final ModelPart namePart = entityDescriptor.findSubPart( "name" );
			assert namePart instanceof BasicValuedSingularAttributeMapping;
			assert "mapping_simple_entity".equals( ( (BasicValuedSingularAttributeMapping) namePart ).getContainingTableExpression() );
			assert "name".equals( ( (BasicValuedSingularAttributeMapping) namePart ).getMappedColumnExpression() );
		}

		{
			final ModelPart genderPart = entityDescriptor.findSubPart( "gender" );
			assert genderPart instanceof BasicValuedSingularAttributeMapping;
			final BasicValuedSingularAttributeMapping genderAttrMapping = (BasicValuedSingularAttributeMapping) genderPart;
			assert "mapping_simple_entity".equals( genderAttrMapping.getContainingTableExpression() );
			assert "gender".equals( genderAttrMapping.getMappedColumnExpression() );
			assert genderAttrMapping.getConverter() != null;
			assert genderAttrMapping.getConverter() instanceof OrdinalEnumValueConverter;
		}

		{
			final ModelPart part = entityDescriptor.findSubPart( "gender2" );
			assert part instanceof BasicValuedSingularAttributeMapping;
			final BasicValuedSingularAttributeMapping attrMapping = (BasicValuedSingularAttributeMapping) part;
			assert "mapping_simple_entity".equals( attrMapping.getContainingTableExpression() );
			assert "gender2".equals( attrMapping.getMappedColumnExpression() );
			assert attrMapping.getConverter() != null;
			assert attrMapping.getConverter() instanceof NamedEnumValueConverter;
		}
	}

	public enum Gender {
		MALE,
		FEMALE
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "mapping_simple_entity" )
	@SuppressWarnings("unused")
	public static class SimpleEntity {
		private Integer id;
		private String name;
		private Gender gender;
		private Gender gender2;

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

		@Enumerated
		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

		@Enumerated( EnumType.STRING )
		public Gender getGender2() {
			return gender2;
		}

		public void setGender2(Gender gender2) {
			this.gender2 = gender2;
		}
	}


}
