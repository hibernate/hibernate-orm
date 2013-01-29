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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import org.osgi.framework.Bundle;

/**
 * OSGI Class loader helper which knows all the interesting bundles and
 * encapsulates the osgi related stuff, especially handles the exceptions which
 * could occure during runtime
 * 
 * @author Martin Neimeier
 */
public class OsgiClassLoaderServiceImpl implements OsgiClassLoaderService {

    private static final long serialVersionUID = 1L;

    /**
     * I know, I could ask the OsgiBundleClassLoaderServiceImpl to get me the
     * contained bundle and use the information in the bundle to generate a key
     * but because of the dynamic nature of osgi I don't want to rely on
     * information which could be gone
     * 
     * @author Martin Neimeier
     */
    private class BundleClassLoaderContainer {
        private OsgiBundleClassLoaderServiceImpl bundleClassLoader;
        private String bundleKey;

        public BundleClassLoaderContainer(Bundle bundle) {
            if (bundle != null) {
                // create a Bundle class loader
                bundleClassLoader = new OsgiBundleClassLoaderServiceImpl(bundle);

                // Create the key
                bundleKey = OsgiClassLoaderServiceImpl.getBundleKey(bundle);
            }
        }

        public OsgiBundleClassLoaderServiceImpl getBundleClassLoader() {
            return bundleClassLoader;
        }

        @SuppressWarnings("unused")
        public String getBundleKey() {
            return bundleKey;
        }

        @Override
        public int hashCode() {
            return bundleKey.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            BundleClassLoaderContainer other = (BundleClassLoaderContainer) obj;
            if (bundleKey == null) {
                if (other.bundleKey != null)
                    return false;
            } else if (!bundleKey.equals(other.bundleKey))
                return false;
            return true;
        }
    }

    private HashMap<String, BundleClassLoaderContainer> bundleClassLoaders;

    public OsgiClassLoaderServiceImpl() {
        bundleClassLoaders = new HashMap<String, BundleClassLoaderContainer>();
    }

    /**
     * load classes .. break on first found match
     */
    @Override
    public <T> Class<T> classForName(String className) {
        synchronized (bundleClassLoaders) {
            // walk over the bundle classloaders and try to load the class
            Iterator<Entry<String, BundleClassLoaderContainer>> it = bundleClassLoaders.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Entry<String, BundleClassLoaderContainer> pair = it.next();

                // try to use this bundle class loader for class loading
                try {
                    Class<T> clazz = pair.getValue()
                            .getBundleClassLoader()
                            .classForName(className);
                    if (clazz != null) {
                        // we found something .. exit
                        return clazz;
                    }
                } catch (Exception ignore) {
                }
            }
            return null;
        }
    }

    /**
     * locate resource .. break on first found match
     */
    @Override
    public URL locateResource(String name) {
        synchronized (bundleClassLoaders) {
            // walk over the bundle classloaders and try to load the class
            Iterator<Entry<String, BundleClassLoaderContainer>> it = bundleClassLoaders.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Entry<String, BundleClassLoaderContainer> pair = it.next();

                // try to use this bundle class loader for class loading
                try {
                    URL url = pair.getValue()
                            .getBundleClassLoader()
                            .locateResource(name);
                    if (url != null) {
                        // we found something .. exit
                        return url;
                    }
                } catch (Exception ignore) {
                }
            }
            return null;
        }
    }

    /**
     * locate resource stream .. break on first found match
     */
    @Override
    public InputStream locateResourceStream(String name) {
        synchronized (bundleClassLoaders) {
            // walk over the bundle class loaders and try to load the class
            Iterator<Entry<String, BundleClassLoaderContainer>> it = bundleClassLoaders.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Entry<String, BundleClassLoaderContainer> pair = it.next();

                // try to use this bundle class loader for class loading
                try {
                    InputStream inputStream = pair.getValue()
                            .getBundleClassLoader()
                            .locateResourceStream(name);
                    if (inputStream != null) {
                        // we found something .. exit
                        return inputStream;
                    }
                } catch (Exception ignore) {
                }
            }
            return null;
        }
    }

    /**
     * locate resource stream .. break on first found match TODO: Should we
     * break on the first matching bundle or continue the search and aggregate
     * all results ?
     */
    @Override
    public List<URL> locateResources(String name) {
        synchronized (bundleClassLoaders) {
            // walk over the bundle class loaders and try to load the class
            Iterator<Entry<String, BundleClassLoaderContainer>> it = bundleClassLoaders.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Entry<String, BundleClassLoaderContainer> pair = it.next();

                // try to use this bundle class loader for class loading
                try {
                    List<URL> list = pair.getValue()
                            .getBundleClassLoader()
                            .locateResources(name);
                    if (list != null) {
                        // we found something .. exit
                        return list;
                    }
                } catch (Exception ignore) {
                }
            }
            return null;
        }
    }

    /**
     * load java services .. break on first found match TODO: Should we break on
     * the first matching bundle or continue the search and aggregate all
     * results ?
     */
    @Override
    public <S> LinkedHashSet<S> loadJavaServices(Class<S> serviceContract) {
        synchronized (bundleClassLoaders) {
            // walk over the bundle class loaders and try to load the class
            Iterator<Entry<String, BundleClassLoaderContainer>> it = bundleClassLoaders.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Entry<String, BundleClassLoaderContainer> pair = it.next();

                // try to use this bundle class loader for class loading
                try {
                    LinkedHashSet<S> set = pair.getValue()
                            .getBundleClassLoader()
                            .loadJavaServices(serviceContract);
                    if (set != null) {
                        // we found something .. exit
                        return set;
                    }
                } catch (Exception ignore) {
                }
            }
            return null;
        }
    }

    /**
     * Register the bundle with this class loader
     */
    @Override
    public void registerBundle(Bundle bundle) {
        if (bundle != null) {
            synchronized (bundleClassLoaders) {
                // create a bundle classloader and add it to the list of
                // classloaders
                String key = getBundleKey(bundle);
                if (!bundleClassLoaders.containsKey(key)) {
                    bundleClassLoaders.put(key,
                            new BundleClassLoaderContainer(bundle));
                }
            }
        }
    }

    /**
     * Unregister the bundle from this class loader
     */
    @Override
    public void unregisterBundle(Bundle bundle) {
        if (bundle != null) {
            synchronized (bundleClassLoaders) {
                // remove a bundle classloader for a given bundle
                String key = getBundleKey(bundle);
                if (bundleClassLoaders.containsKey(key)) {
                    bundleClassLoaders.remove(key);
                }
            }
        }
    }

    protected static String getBundleKey(Bundle bundle) {
        return bundle.getSymbolicName() + " " + bundle.getVersion().toString();
    }

}