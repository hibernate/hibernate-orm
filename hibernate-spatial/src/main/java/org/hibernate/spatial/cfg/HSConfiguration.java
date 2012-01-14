/**
 * $Id: HSConfiguration.java 134 2009-06-22 20:41:53Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *
 * Copyright © 2007 Geovise BVBA
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
package org.hibernate.spatial.cfg;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hibernate.cfg.Configuration;
import org.hibernate.spatial.Log;
import org.hibernate.spatial.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Configuration information for the Hibernate Spatial Extension.
 *
 * @author Karel Maesen
 */
public class HSConfiguration extends Properties {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static Log logger = LogFactory.make();

	private String source = "runtime configuration object";

	private HSProperty[] HSProperties;

	public HSConfiguration() {
		HSProperties = HSProperty.values();
	}

	public String getDefaultDialect() {
		return getProperty( HSProperty.DEFAULT_DIALECT.toString() );
	}

	public void setDefaultDialect(String dialect) {
		setProperty( HSProperty.DEFAULT_DIALECT, dialect );
	}

	public String getPrecisionModel() {
		return getProperty( HSProperty.PRECISION_MODEL.toString() );
	}

	public void setPrecisionModel(String precisionModel) {
		setProperty( HSProperty.PRECISION_MODEL, precisionModel );
	}

	public String getPrecisionModelScale() {
		return getProperty( HSProperty.PRECISION_MODEL_SCALE.toString() );
	}

	public void setPrecisionModelScale(String scale) {
		setProperty( HSProperty.PRECISION_MODEL_SCALE, scale );
	}

	protected String getProperty(HSProperty property) {
		return getProperty( property.toString() );
	}

	protected void setProperty(HSProperty property, String value) {
		setProperty( property.toString(), value );
	}

	/**
	 * Derives the configuration from the Hibernate Configuration object.
	 *
	 * @param hibernateConfig Hibernate Configuration object
	 *
	 * @return true, if the configuration is successfull.
	 */
	public boolean configure(Configuration hibernateConfig) {
		String dialect = hibernateConfig.getProperty( "hibernate.dialect" );
		setProperty( HSProperty.DEFAULT_DIALECT, dialect );
		return true;
	}

	/**
	 * Gets the configuriation from the hibernate-spatail.cfg.xml file on the
	 * classpath.
	 *
	 * @return true if the configuration is successfull;
	 */
	public boolean configure() {
		return configure( "hibernate-spatial.cfg.xml" );
	}

	/**
	 * Gets the configuriation from the specified file.
	 *
	 * @param resource the configuration file
	 *
	 * @return true if the configuration is successfull;
	 */
	public boolean configure(File resource) {
		logger.info(
				"Attempting to configuring from file: "
						+ resource.getName()
		);
		try {
			this.source = resource.getAbsolutePath();
			return doConfigure( new FileInputStream( resource ) );
		}
		catch ( FileNotFoundException e ) {
			logger.warn( "could not find file: " + resource + "." );
		}
		catch ( DocumentException e ) {
			logger.warn(
					"Failed to load configuration file: " + resource
							+ ".\nCause:" + e.getMessage()
			);
		}
		return false;
	}

	/**
	 * The source file or URL for this configuration.
	 *
	 * @return The source name (file or URL).
	 */
	public String getSource() {
		return this.source;
	}

	/**
	 * Gets the configuriation from the specified file on the class path.
	 *
	 * @param resource the configuration file
	 *
	 * @return true if the configuration is successfull;
	 */
	public boolean configure(String resource) {
		logger.debug( "Attempting to load configuration from file: " + resource );
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			URL url = classLoader.getResource( resource );
			if ( url == null ) {
				logger.info(
						"No configuration file " + resource
								+ " on the classpath."
				);
				return false;
			}
			this.source = url.getFile();
			return doConfigure( url.openStream() );
		}
		catch ( Exception e ) {
			logger.warn(
					"Failed to load configuration file: " + resource
							+ ".\nCause:" + e.getMessage()
			);
		}
		return false;
	}

	private boolean doConfigure(InputStream stream) throws DocumentException {
		try {
			SAXReader reader = new SAXReader();
			Document configDoc = reader.read( stream );
			Element root = configDoc.getRootElement();
			for ( HSProperty hsprop : HSProperties ) {
				Element propEl = root.element( hsprop.toString().toLowerCase() );
				if ( propEl != null ) {
					setProperty( hsprop, propEl.getText() );
				}
			}
			return true;
		}
		finally {
			try {
				stream.close();
			}
			catch ( Exception e ) {
			} // Can't do anything about this.
		}
	}

	public String toString() {
		return this.source;
	}

}
