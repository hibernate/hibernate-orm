package org.hibernate.test.annotations.lob;

import org.hibernate.Session;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.Type;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test type definition for SerializableToBlobType
 * 
 * @author Janario Oliveira
 */
@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
public class SerializableToBlobTypeTest extends BaseCoreFunctionalTestCase {
	@Test
	@FailureExpectedWithNewMetamodel
	public void testTypeDefinition() {
		EntityBinding binding = metadata().getEntityBinding( EntitySerialize.class.getName() );

		// explicitLob of SerializableToBlobType
		Type explicitLobType = binding.locateAttributeBinding( "explicitLob" )
				.getHibernateTypeDescriptor().getResolvedTypeMapping();
		assertEquals( ExplicitSerializable.class, explicitLobType.getReturnedClass() );
		assertEquals( SerializableToBlobType.class.getName(), explicitLobType.getName() );

		// explicit of ExplicitSerializableType
		Type explicitType = binding.locateAttributeBinding( "explicit" )
				.getHibernateTypeDescriptor().getResolvedTypeMapping();
		assertEquals( ExplicitSerializable.class, explicitType.getReturnedClass() );
		assertEquals( ExplicitSerializableType.class.getName(), explicitType.getName() );

		// implicit of ImplicitSerializableType
		Type implicitType = binding.locateAttributeBinding( "implicit" )
				.getHibernateTypeDescriptor().getResolvedTypeMapping();
		assertEquals( ImplicitSerializable.class, implicitType.getReturnedClass() );
		assertEquals( ImplicitSerializableType.class.getName(), implicitType.getName() );

		// explicitOverridingImplicit ExplicitSerializableType overrides ImplicitSerializableType
		Type overrideType = binding.locateAttributeBinding( "explicitOverridingImplicit" )
				.getHibernateTypeDescriptor().getResolvedTypeMapping();
		assertEquals( ImplicitSerializable.class, overrideType.getReturnedClass() );
		assertEquals( ExplicitSerializableType.class.getName(), overrideType.getName() );
	}

	@Test
	public void testPersist() {
		EntitySerialize entitySerialize = new EntitySerialize();

		entitySerialize.explicitLob = new ExplicitSerializable();
		entitySerialize.explicitLob.value = "explicitLob";
		entitySerialize.explicitLob.defaultValue = "defaultExplicitLob";

		entitySerialize.explicit = new ExplicitSerializable();
		entitySerialize.explicit.value = "explicit";

		entitySerialize.implicit = new ImplicitSerializable();
		entitySerialize.implicit.value = "implicit";

		entitySerialize.explicitOverridingImplicit = new ImplicitSerializable();
		entitySerialize.explicitOverridingImplicit.value = "explicitOverridingImplicit";

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( entitySerialize );

		session.getTransaction().commit();
		session.close();

		session = openSession();

		EntitySerialize persistedSerialize = (EntitySerialize) session.get( EntitySerialize.class, entitySerialize.id );
		assertEquals( "explicitLob", persistedSerialize.explicitLob.value );
		assertEquals( "explicit", persistedSerialize.explicit.value );
		assertEquals( "implicit", persistedSerialize.implicit.value );
		assertEquals( "explicitOverridingImplicit", persistedSerialize.explicitOverridingImplicit.value );

		assertEquals( "defaultExplicitLob", persistedSerialize.explicitLob.defaultValue );

		session.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { EntitySerialize.class };
	}
}
