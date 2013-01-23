/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.osgi.internal;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.jboss.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * @author Martin Neimeier
 */
public class OsgiBundleClassLoaderServiceImpl implements ClassLoaderService {
    private static final Logger log = Logger.getLogger(OsgiBundleClassLoaderServiceImpl.class);

    private static final long serialVersionUID = 1L;
    private Bundle bundle;

    /**
     * Create a new class loader service for an osgi bundle
     * 
     * @param bundle
     *            The bundle for which this ClassLoaderService is responsible
     */
    public OsgiBundleClassLoaderServiceImpl(Bundle bundle) {
        log.tracef("Instanciate classloader service for bundle [%s], verson [%s]",
                bundle.getSymbolicName(),
                bundle.getVersion());
        this.bundle = bundle;
    }

    /**
     * Check if the stored bundle is ready in terms of class loading
     * 
     * @return true, if the bundle could be used for class loading, false
     *         otherwise
     */
    private boolean isReady() {
        if (bundle != null) {
            try {
                // a bundle is ready in terms of class loading when it is beyond
                // the resolved state
                int state = bundle.getState();
                if ((state & Bundle.RESOLVED) != 0 || (state & Bundle.STARTING) != 0
                        || (state & Bundle.STOPPING) != 0
                        || (state & Bundle.ACTIVE) != 0) {
                    return true;
                } else {
                    log.tracef("Bundle %s is in wrong state for class loading",
                            bundle.getSymbolicName());
                }
            } catch (RuntimeException e) {
                throw new ClassLoadingException("Bundle [" + bundle.getSymbolicName()
                        + "] not ready for class loading",
                        e);
            }
        }
        return false;
    }

    /**
     * Use the bundle to try to resolve classes
     * 
     * @see org.hibernate.boot.registry.classloading.spi.ClassLoaderService#classForName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> classForName(String className) {
        if (isReady()) {
            try {
                Class<T> clazz = bundle.loadClass(className);
                log.tracef("Bundle [%s] used for loading class [%s]", bundle.getSymbolicName(), clazz);
                
                return clazz;
            } catch (Exception e) {
                throw new ClassLoadingException("Unable to load class [" + className
                        + "]",
                        e);
            }
        }
        return null;
    }

    @Override
    public URL locateResource(String name) {
        if (isReady()) {
            try {
                URL url = bundle.getResource(name);
                log.tracef("Bundle [%s] used for loading resource [%s]", bundle.getSymbolicName(), url);
                return url;
            } catch (Exception e) {
                throw new ClassLoadingException("Unable to locate resource [" + name
                        + "]",
                        e);
            }
        }
        return null;
    }

    @Override
    public InputStream locateResourceStream(String name) {
        // TODO: Not yet implemented
        return null;
    }

    @Override
    public List<URL> locateResources(String name) {
        if (isReady()) {
            try {
                List<URL> result = new ArrayList<URL>();
                @SuppressWarnings("unchecked")
                Enumeration<URL> resources = bundle.getResources(name);
                while (resources.hasMoreElements()) {

                    result.add(resources.nextElement());
                }
                return result;

            } catch (Exception e) {
                throw new ClassLoadingException("Unable to locate resources [" + name
                        + "]",
                        e);
            }
        }
        return null;
    }

    /**
     * Iterate over the "osgi" services this bundle has registered, and check if
     * one or more of the services implements the desired contract - a kind of
     * OSGI Service to java-service-Bridge. 
     * Only problem here is that the lifetime of the returned services could not 
     * be guaranteed. There could be a RuntimeException when the service is used
     */
    @Override
    public <S> LinkedHashSet<S> loadJavaServices(Class<S> serviceContract) {
        if (isReady()) {
            LinkedHashSet<S> hashSet = new LinkedHashSet<S>();

            // get the services this bundle has registered
            ServiceReference serviceReference[] = bundle.getRegisteredServices();
            if (serviceReference != null) {
                for (ServiceReference reference : serviceReference) {
                    // check if the specified bundle use the same source
                    // for the package of the specified serviceContract
                    if (reference.isAssignableTo(bundle,
                            serviceContract.getName())) {
                        // yes we can !!
                        // get the service via the bundleContext
                        Object serviceObject = bundle.getBundleContext()
                                .getService(reference);
                        if (serviceObject != null) {
                            if (serviceContract.isInstance(serviceObject)) {
                                @SuppressWarnings("unchecked")
                                S service = (S)serviceObject;
                                log.tracef("Service of class [%s] matches contract [%s]",
                                        service.getClass().getName(),
                                        serviceContract.getName());
                                
                                hashSet.add(service);
                            }
                        } else {
                            log.tracef("Cannot get service object for service reference [%s]",
                                    reference);
                        }

                    } else {
                        log.tracef("Bundle [%s] is not assignable to the package of contract [%s]",
                                bundle.getSymbolicName(), serviceContract.getName());
                    }
                    
                }
            } else {
                log.tracef("Bundle [%s] hasn't registered any services yet", bundle.getSymbolicName());
            }
            return hashSet;
        } 
        return null;
    }
}