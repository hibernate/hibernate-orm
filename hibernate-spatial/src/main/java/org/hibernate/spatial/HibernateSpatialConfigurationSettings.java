/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;

import java.io.Serializable;

import org.jboss.logging.Logger;

/**
 * Hibernate Spatial specific configuration settings.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/16/13
 */
public class HibernateSpatialConfigurationSettings implements Serializable {

	/**
	 * The canonical class name to use as Oracle ConnectionFinder implementation.
	 */
	public static final String CONNECTION_FINDER = "hibernate.spatial.connection_finder";



}
