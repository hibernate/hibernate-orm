package org.hibernate.test.annotations.enumerated;

import static org.junit.Assert.assertEquals;

import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.test.annotations.enumerated.EntityEnum.Common;
import org.hibernate.test.annotations.enumerated.EntityEnum.FirstLetter;
import org.hibernate.test.annotations.enumerated.EntityEnum.LastNumber;
import org.hibernate.type.EnumType;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test type definition for enum with ServiceRegistry
 * 
 * @author Janario Oliveira
 */
public class EnumeratedTypeServiceRegistryTest extends EnumeratedTypeTest {

	private AttributeBinding getAttributeBinding(String name, EntityBinding inEntityBinding) {
		for ( AttributeBinding attributeBinding : inEntityBinding.getAttributeBindingClosure() ) {
			if ( attributeBinding.getAttribute().getName().equals( name ) ) {
				return attributeBinding;
			}
		}
		return null;
	}

	protected void configure(Configuration configuration) {
		configuration.setProperty( USE_NEW_METADATA_MAPPINGS, "true" );
	}

	@Override
	@Ignore("Super test is tested with Configuration")
	public void testTypeDefinition() {
	}

	@Test
	public void typeDefinition_withServiceRegistry() {
		MetadataImplementor metadata = metadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityEnum.class.getName() );
		// ordinal default of EnumType
		HibernateTypeDescriptor ordinalEnum = getAttributeBinding( "ordinal", entityBinding )
				.getHibernateTypeDescriptor();
		assertEquals( Common.class.getName(), ordinalEnum.getJavaTypeName() );
		assertEquals( EnumType.class.getName(), ordinalEnum.getExplicitTypeName() );

		// string defined by Enumerated(STRING)
		HibernateTypeDescriptor stringEnum = getAttributeBinding( "string", entityBinding )
				.getHibernateTypeDescriptor();
		assertEquals( Common.class.getName(), stringEnum.getJavaTypeName() );
		assertEquals( EnumType.class.getName(), stringEnum.getExplicitTypeName() );

		// explicit defined by @Type
		HibernateTypeDescriptor first = getAttributeBinding( "firstLetter", entityBinding )
				.getHibernateTypeDescriptor();
		assertEquals( FirstLetter.class.getName(), first.getJavaTypeName() );
		assertEquals( FirstLetterType.class.getName(), first.getExplicitTypeName() );

		// implicit defined by @TypeDef in somewhere
		HibernateTypeDescriptor last = getAttributeBinding( "lastNumber", entityBinding ).getHibernateTypeDescriptor();
		assertEquals( LastNumber.class.getName(), last.getJavaTypeName() );
		assertEquals( LastNumberType.class.getName(), last.getExplicitTypeName() );

		// implicit defined by @TypeDef in anywhere, but overrided by Enumerated(STRING)
		HibernateTypeDescriptor implicitOverrideExplicit = getAttributeBinding( "explicitOverridingImplicit",
				entityBinding ).getHibernateTypeDescriptor();
		assertEquals( LastNumber.class.getName(), implicitOverrideExplicit.getJavaTypeName() );
		assertEquals( EnumType.class.getName(), implicitOverrideExplicit.getExplicitTypeName() );
	}
}
