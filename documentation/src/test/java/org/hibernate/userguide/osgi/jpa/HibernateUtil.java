package org.hibernate.userguide.osgi.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

//tag::osgi-discover-EntityManagerFactory[]
public class HibernateUtil {

    private EntityManagerFactory emf;

    public EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    private EntityManagerFactory getEntityManagerFactory() {
        if ( emf == null ) {
            Bundle thisBundle = FrameworkUtil.getBundle(
                HibernateUtil.class
            );
            BundleContext context = thisBundle.getBundleContext();

            ServiceReference serviceReference = context.getServiceReference(
                PersistenceProvider.class.getName()
            );
            PersistenceProvider persistenceProvider = ( PersistenceProvider ) context
            .getService(
                serviceReference
            );

            emf = persistenceProvider.createEntityManagerFactory(
                "YourPersistenceUnitName",
                null
            );
        }
        return emf;
    }
}
//end::osgi-discover-EntityManagerFactory[]