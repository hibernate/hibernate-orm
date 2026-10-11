package org.hibernate.orm.test.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.internal.InaccessibleConstructorProxyFactory;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-20977" )
public class InaccessibleConstructorTests {

	@Test
	@ServiceRegistry
	void testPrivateCtor(ServiceRegistryScope registryScope) {
		var registry = registryScope.getRegistry();
		var domainModel = new MetadataSources( registry ).addAnnotatedClass( PrivateCtorEntity.class ).buildMetadata();

		try ( var sf = domainModel.buildSessionFactory() ) {
			var mapping = sf.unwrap( SessionFactoryImplementor.class )
					.getMappingMetamodel()
					.findEntityDescriptor( PrivateCtorEntity.class );
			assertThat( mapping.getRepresentationStrategy().getProxyFactory() )
					.isInstanceOf( InaccessibleConstructorProxyFactory.class );

			try {
				sf.inSession( (session) -> {
					session.getReference( PrivateCtorEntity.class, 1 );
				} );
				fail("Expecting a failure");
			}
			catch (HibernateException error) {
				assertThat( error.getMessage() )
						.startsWith( "Unable to create runtime proxy due to missing public, protected or package-private default constructor" );
			}
		}
	}

	@Entity
	public static class PrivateCtorEntity {
		private PrivateCtorEntity() {
		}

		@Id
		private Long id;
		private String name;
	}

}
