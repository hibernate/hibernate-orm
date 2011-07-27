/**
 * $Id: HBSpatialExtension.java 253 2010-10-02 15:14:52Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *
 * Copyright © 2007 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission, 
 * under the 6th Framework Programme, contract IST-2-004688-STP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.spatial.cfg.HSConfiguration;
import org.hibernate.spatial.helper.GeometryFactoryHelper;
import org.hibernate.spatial.helper.PropertyFileReader;
import org.hibernate.spatial.mgeom.MGeometryFactory;
import org.hibernate.spatial.spi.SpatialDialectProvider;

/**
 * This is the bootstrap class that is used to get an
 * <code>SpatialDialect</code>.
 * <p/>
 * It also provides a default <code>SpatialDialect</code>.
 * <code>GeometryType</code>s that do not have a <code>dialect</code>
 * parameter use this default.
 * <p/>
 * The default <code>SpatialDialect</code> will be the first one that is
 * returned by the <code>getDefaultDialect</code> method of the provider at
 * least if it is non null.
 *
 * @author Karel Maesen
 */

//TODO -- this should be moved to the
public class HBSpatialExtension {

	protected static List<SpatialDialectProvider> providers = new ArrayList<SpatialDialectProvider>();

	private static final Logger log = LoggerFactory.getLogger( HBSpatialExtension.class );

	private static SpatialDialect defaultSpatialDialect = null;

	private static final String DIALECT_PROP_NAME = "hibernate.spatial.dialect";

	private static HSConfiguration configuration = null;

	private static MGeometryFactory defaultGeomFactory = new MGeometryFactory();

	private static boolean configured = false;

	static {

		log.info( "Initializing HBSpatialExtension" );
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> resources = null;
		try {
			resources = loader.getResources(
					"META-INF/services/"
							+ SpatialDialectProvider.class.getName()
			);
			Set<String> names = new HashSet<String>();
			while ( resources.hasMoreElements() ) {
				URL url = resources.nextElement();
				InputStream is = url.openStream();
				try {
					names.addAll( providerNamesFromReader( is ) );
				}
				finally {
					is.close();
				}
			}

			for ( String s : names ) {
				try {
					log.info(
							"Attempting to load Hibernate Spatial Provider "
									+ s
					);
					SpatialDialectProvider provider = (SpatialDialectProvider) loader
							.loadClass( s ).newInstance();
					providers.add( provider );
				}
				catch ( Exception e ) {
					throw new HibernateSpatialException(
							"Problem loading provider class", e
					);
				}

			}
		}
		catch ( IOException e ) {
			throw new HibernateSpatialException(
					"No "
							+ SpatialDialectProvider.class.getName()
							+ " found in META-INF/services", e
			);
		}

		// configuration - check if there is a system property
		String dialectProp = System.getProperty( DIALECT_PROP_NAME );
		if ( dialectProp != null ) {
			HSConfiguration hsConfig = new HSConfiguration();
			hsConfig.setDefaultDialect( dialectProp );
			setConfiguration( hsConfig );
		}

		// configuration - load the config file
		log.info( "Checking for default configuration file." );
		HSConfiguration hsConfig = new HSConfiguration();
		if ( hsConfig.configure() ) {
			configuration = hsConfig;
		}

	}

	/**
	 * Make sure nobody can instantiate this class
	 */
	private HBSpatialExtension() {
	}

	public static void setConfiguration(HSConfiguration c) {
		log.info( "Setting configuration object:" + c );
		configuration = c;
		//if the HSExtension has already been initialized,
		//then it should be reconfigured.
		if ( configured == true ) {
			forceConfigure();
		}
	}

	private static synchronized void configure() {
//		// do nothing if already configured
		if ( configured ) {
			return;
		}
		configured = true;
		forceConfigure();


	}

	private static void forceConfigure() {
		// if no configuration object, take the first dialect that is available.
		if ( configuration == null ) {
			return;
		}
		else {
			log.info(
					"Configuring HBSpatialExtension from "
							+ configuration.getSource()
			);
			String dialectName = configuration.getDefaultDialect();
			if ( dialectName != null ) {
				SpatialDialect dialect = createSpatialDialect( dialectName );
				if ( dialect != null ) {
					log.info( "Setting Spatial Dialect to : " + dialectName );
					setDefaultSpatialDialect( dialect );
				}
			}

			// trying to create a defaultGeometryFactory
			log.info( "Creating default Geometry Factory" );
			defaultGeomFactory = GeometryFactoryHelper
					.createGeometryFactory( configuration );

		}

		if ( defaultSpatialDialect == null ) {
			log.warn( "Hibernate Spatial Configured but no spatial dialect" );
		}
		else {
			log.info(
					"Hibernate Spatial configured. Using dialect: "
							+ defaultSpatialDialect.getClass().getCanonicalName()
			);
		}
	}

	public static HSConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * @param dialect
	 */
	private static void setDefaultSpatialDialect(SpatialDialect dialect) {
		defaultSpatialDialect = dialect;
	}

	public static SpatialDialect getDefaultSpatialDialect() {
		configure();
		return defaultSpatialDialect;
	}

	public static SpatialDialect createSpatialDialect(String dialectName) {
		SpatialDialect dialect = null;
		for ( SpatialDialectProvider provider : providers ) {
			dialect = provider.createSpatialDialect( dialectName );
			if ( dialect != null ) {
				break;
			}
		}
		if ( dialect == null ) {
			throw new HibernateSpatialException(
					"No SpatialDialect provider for persistenceUnit "
							+ dialectName
			);
		}
		return dialect;
	}

	//TODO -- this is not thread-safe!
	//find another way to initialize

	public static MGeometryFactory getDefaultGeomFactory() {
		configure();
		return defaultGeomFactory;
	}

	// Helper methods

	private static Set<String> providerNamesFromReader(InputStream is)
			throws IOException {
		PropertyFileReader reader = new PropertyFileReader( is );
		return reader.getNonCommentLines();
	}

}
