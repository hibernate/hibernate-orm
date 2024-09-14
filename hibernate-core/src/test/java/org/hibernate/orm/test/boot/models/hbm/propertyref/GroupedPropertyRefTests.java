/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.models.hbm.propertyref;

import java.util.List;

import org.hibernate.boot.InvalidMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code hbm.xml} {@code <properties/>} grouping element
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class GroupedPropertyRefTests {
	@Test
	@DomainModel(annotatedClasses = {Person.class, Account.class}, xmlMappings = "mappings/models/hbm/propertyref/properties.hbm.xml" )
	@SessionFactory
	void testHbmXml(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		// baseline test for straight hbm.xml handling
		try {
			verify( domainModelScope, sessionFactoryScope );
		}
		finally {
			sessionFactoryScope.inTransaction( (session) -> {
				session.createMutationQuery( "delete GroupedPropertyRefTests$Account" ).executeUpdate();
				session.createMutationQuery( "delete GroupedPropertyRefTests$Person" ).executeUpdate();
			} );
		}
	}

	private void verify(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		final PersistentClass accountMapping = domainModelScope.getEntityBinding( Account.class );

		final Property ownerProperty = accountMapping.getProperty( "owner" );
		final ToOne ownerPropertyValue = (ToOne) ownerProperty.getValue();
		assertThat( ownerPropertyValue.getReferencedPropertyName() ).isEqualTo( "name" );

		sessionFactoryScope.inTransaction( (session) -> {
			final Person john = new Person( 1, "John", "Doe" );
			final Account account = new Account( 1, "savings", john );
			session.persist( john );
			session.persist( account );
		} );

		sessionFactoryScope.inTransaction( (session) -> {
			final List<Account> accounts = session.createSelectionQuery(
					"from GroupedPropertyRefTests$Account a join fetch a.owner",
					Account.class ).list();
			assertThat( accounts ).hasSize( 1 );
		} );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name= MappingSettings.TRANSFORM_HBM_XML, value="true"))
	void testTransformed(ServiceRegistryScope registryScope) {
		// test the transformation - should be an error as this is unsupported
		try {
			final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();
			final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			metadataSources.addResource( "mappings/models/hbm/propertyref/properties.hbm.xml" );
		}
		catch (InvalidMappingException expected) {
			assertThat( expected.getCause() ).isInstanceOf( UnsupportedOperationException.class );
			assertThat( expected.getCause().getMessage() ).startsWith( "<properties/>" );
		}
	}

	public static class Person {
		private Integer id;
		private String firstName;
		private String lastName;

		public Person() {
		}

		public Person(Integer id, String firstName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public Integer getId() {
			return id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}

	public static class Account {
		private Integer id;
		private String name;
		private Person owner;

		public Account() {
		}

		public Account(Integer id, String name, Person owner) {
			this.id = id;
			this.name = name;
			this.owner = owner;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}
	}

}
