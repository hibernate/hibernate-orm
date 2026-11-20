package org.hibernate.orm.test.mapping.cascade;

import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadingActions;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a default cascade declared in a {@code mapping.xml} document, either at the
 * {@code <entity-mappings>} root level or in {@code <persistence-unit-defaults>}, is applied
 * to associations which do not declare an explicit cascade and is added to any explicit
 * {@code <cascade/>}.
 * <p>
 * A root level default applies only to the document declaring it, while a persistence-unit
 * default applies to every document.
 */
@BaseUnitTest
@JiraKey( "HHH-21074" )
class XmlDefaultCascadeTest {

	private static final String NO_DEFAULT_CASCADE = "org/hibernate/orm/test/mapping/cascade/no-default-cascade.orm.xml";

	@Test
	void testEntityMappingsRootDefaultCascade() {
		final Metadata metadata = buildMetadata(
				"org/hibernate/orm/test/mapping/cascade/root-default-cascade.orm.xml"
		);
		verifyParent( metadata );

		// the root default-cascade does not leak into other documents
		// Holder is declared in no-default-cascade.orm.xml, which has no default-cascade
		// so the default cascade ("merge") defined in root-default-cascade.orm.xml is not applied to Holder.items
		final Property items = metadata.getEntityBinding( Holder.class.getName() ).getProperty( "items" );
		assertCascades( items, false, false, false );
	}

	@Test
	void testPersistenceUnitDefaultsDefaultCascade() {
		final Metadata metadata = buildMetadata(
				"org/hibernate/orm/test/mapping/cascade/pu-default-cascade.orm.xml"
		);
		verifyParent( metadata );

		// the persistence-unit default-cascade applies to every document
		// Holder is declared in no-default-cascade.orm.xml, which has no default-cascade
		// so the default cascade ("merge") defined in pu-default-cascade.orm.xml is applied to Holder.items
		final Property items = metadata.getEntityBinding( Holder.class.getName() ).getProperty( "items" );
		assertCascades( items, false, true, false );
	}

	private static Metadata buildMetadata(String resource) {
		final StandardServiceRegistry ssr = ServiceRegistryBuilder.buildServiceRegistry();
		try {
			return new MetadataSources( ssr )
					.addResource( resource )
					.addResource( NO_DEFAULT_CASCADE )
					.buildMetadata();
		}
		finally {
			ServiceRegistryBuilder.destroy( ssr );
		}
	}

	private static void verifyParent(Metadata metadata) {
		final PersistentClass parent = metadata.getEntityBinding( Parent.class.getName() );

		// no explicit cascade: only the default cascade ("merge")
		assertCascades( parent.getProperty( "children" ), false, true, false );

		// explicit cascade ("persist"): the default cascade ("merge") is added
		assertCascades( parent.getProperty( "others" ), true, true, false );

		// empty explicit cascade (<cascade/>): the default cascade ("merge") is added
		assertCascades( parent.getProperty( "unrelated" ), false, true, false );
	}

	private static void assertCascades(Property property, boolean isPersistCascade, boolean isMergeCascade, boolean isRemoveCascade) {
		assertCascade( property, CascadingActions.PERSIST, isPersistCascade );
		assertCascade( property, CascadingActions.MERGE, isMergeCascade );
		assertCascade( property, CascadingActions.REMOVE, isRemoveCascade );
	}

	private static void assertCascade(Property property, CascadingAction<?> action, boolean expected) {
		assertThat( property.getCascadeStyle().doCascade( action ) )
				.as( "%s cascade style %s, action %s", property.getName(), property.getCascadeStyle(), action )
				.isEqualTo( expected );
	}

	public static class Parent {
		private Integer id;
		private Set<Child> children;
		private Set<Other> others;
		private Set<Unrelated> unrelated;
	}

	public static class Child {
		private Integer id;
	}

	public static class Other {
		private Integer id;
	}

	public static class Unrelated {
		private Integer id;
	}

	public static class Holder {
		private Integer id;
		private Set<Child> items;
	}
}
