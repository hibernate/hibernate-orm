/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Creating a new version of {@link InstantiationTests} that won't fail the build,
 * but still allows us to test the behavior.
 *
 * JUnit does not like when build fixtures (here, the SF) fails
 */
@ServiceRegistry
@FailureExpected( jiraKey = "HHH-14950", reason = "Model has no setters, which is not supported" )
@JiraKey( "HHH-14950" )
public class TempInstantiationTests {
	@Test
	public void basicTest(ServiceRegistryScope registerScope) {
		final MetadataSources metadataSources = new MetadataSources( registerScope.getRegistry() )
				.addAnnotatedClass( Person.class )
				.addAnnotatedClass( Name.class )
				.addAnnotatedClass( NameImpl.class );

		final Metadata metadata = metadataSources.buildMetadata();

		final PersistentClass personMapping = metadata.getEntityBinding( Person.class.getName() );

		final Property name = personMapping.getProperty( "name" );
		final Component nameMapping = (Component) name.getValue();
		assertThat( nameMapping.getPropertySpan() ).isEqualTo( 2 );

		final Property aliases = personMapping.getProperty( "aliases" );
		final Component aliasMapping = (Component) ( (Collection) aliases.getValue() ).getElement();
		assertThat( aliasMapping.getPropertySpan() ).isEqualTo( 2 );
	}
}
