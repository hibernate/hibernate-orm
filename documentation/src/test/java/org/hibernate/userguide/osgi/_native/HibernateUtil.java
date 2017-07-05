package org.hibernate.userguide.osgi._native;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

//tag::osgi-discover-SessionFactory[]
public class HibernateUtil {

    private SessionFactory sf;

    public Session getSession() {
        return getSessionFactory().openSession();
    }

    private SessionFactory getSessionFactory() {
        if ( sf == null ) {
            Bundle thisBundle = FrameworkUtil.getBundle(
                HibernateUtil.class
            );
            BundleContext context = thisBundle.getBundleContext();

            ServiceReference sr = context.getServiceReference(
                SessionFactory.class.getName()
            );
            sf = ( SessionFactory ) context.getService( sr );
        }
        return sf;
    }
}
//end::osgi-discover-SessionFactory[]