/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.multitenancy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.util.DdlTransactionIsolatorTestingImpl;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractMultiTenancyTest extends BaseUnitTestCase {

    protected static final String FRONT_END_TENANT = "front_end";
    protected static final String BACK_END_TENANT = "back_end";

    private Map<String, ConnectionProvider> connectionProviderMap = new HashMap<>(  );

    private SessionFactory sessionFactory;

    public AbstractMultiTenancyTest() {
        init();
    }

    //tag::multitenacy-hibernate-MultiTenantConnectionProvider-example[]
    private void init() {
        registerConnectionProvider( FRONT_END_TENANT );
        registerConnectionProvider( BACK_END_TENANT );

        Map<String, Object> settings = new HashMap<>(  );

        settings.put( AvailableSettings.MULTI_TENANT, multiTenancyStrategy() );
        settings.put( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
            new ConfigurableMultiTenantConnectionProvider( connectionProviderMap ) );

        sessionFactory = sessionFactory(settings);
    }
    //end::multitenacy-hibernate-MultiTenantConnectionProvider-example[]

    @AfterClassOnce
    public void destroy() {
        sessionFactory.close();
        for ( ConnectionProvider connectionProvider : connectionProviderMap.values() ) {
            if ( connectionProvider instanceof Stoppable ) {
                ( (Stoppable) connectionProvider ).stop();
            }
        }
    }

    //tag::multitenacy-hibernate-MultiTenantConnectionProvider-example[]

    protected void registerConnectionProvider(String tenantIdentifier) {
        Properties properties = properties();
        properties.put( Environment.URL,
            tenantUrl(properties.getProperty( Environment.URL ), tenantIdentifier) );

        DriverManagerConnectionProviderImpl connectionProvider =
            new DriverManagerConnectionProviderImpl();
        connectionProvider.configure( properties );
        connectionProviderMap.put( tenantIdentifier, connectionProvider );
    }
    //end::multitenacy-hibernate-MultiTenantConnectionProvider-example[]

    @Test
    public void testBasicExpectedBehavior() {

		//tag::multitenacy-multitenacy-hibernate-same-entity-example[]
        doInSession( FRONT_END_TENANT, session -> {
            Person person = new Person(  );
            person.setId( 1L );
            person.setName( "John Doe" );
            session.persist( person );
        } );

        doInSession( BACK_END_TENANT, session -> {
            Person person = new Person(  );
            person.setId( 1L );
            person.setName( "John Doe" );
            session.persist( person );
        } );
		//end::multitenacy-multitenacy-hibernate-same-entity-example[]
    }

    protected abstract MultiTenancyStrategy multiTenancyStrategy();

    protected Properties properties() {
        Properties properties = new Properties( );
        URL propertiesURL = Thread.currentThread().getContextClassLoader().getResource( "hibernate.properties" );
        try(FileInputStream inputStream = new FileInputStream( propertiesURL.getFile() )) {
            properties.load( inputStream );
        }
        catch (IOException e) {
            throw new IllegalArgumentException( e );
        }
        return properties;
    }

    protected abstract String tenantUrl(String originalUrl, String tenantIdentifier);

    protected SessionFactory sessionFactory(Map<String, Object> settings) {

        ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
            .applySettings( settings )
            .build();

        MetadataSources metadataSources = new MetadataSources( serviceRegistry );
        for(Class annotatedClasses : getAnnotatedClasses()) {
            metadataSources.addAnnotatedClass( annotatedClasses );
        }

        Metadata metadata = metadataSources.buildMetadata();

        HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
        tool.injectServices( serviceRegistry );

        final GenerationTargetToDatabase frontEndSchemaGenerator =  new GenerationTargetToDatabase(
                new DdlTransactionIsolatorTestingImpl(
                        serviceRegistry,
                        connectionProviderMap.get( FRONT_END_TENANT )
                )
        );
        final GenerationTargetToDatabase backEndSchemaGenerator = new GenerationTargetToDatabase(
                new DdlTransactionIsolatorTestingImpl(
                        serviceRegistry,
                        connectionProviderMap.get( BACK_END_TENANT )
                )
        );

        new SchemaDropperImpl( serviceRegistry ).doDrop(
                metadata,
                serviceRegistry,
                settings,
                true,
                frontEndSchemaGenerator,
                backEndSchemaGenerator
        );

        new SchemaCreatorImpl( serviceRegistry ).doCreation(
                metadata,
                serviceRegistry,
                settings,
                true,
                frontEndSchemaGenerator,
                backEndSchemaGenerator
        );

        final SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
        return sessionFactoryBuilder.build();
    }

    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Person.class
        };
    }

	//tag::multitenacy-hibernate-session-example[]
    private void doInSession(String tenant, Consumer<Session> function) {
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory
                .withOptions()
                .tenantIdentifier( tenant )
                .openSession();
            txn = session.getTransaction();
            txn.begin();
            function.accept(session);
            txn.commit();
        } catch (Throwable e) {
            if ( txn != null ) txn.rollback();
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
	//end::multitenacy-hibernate-session-example[]

    @Entity(name = "Person")
    public static class Person {

        @Id
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
