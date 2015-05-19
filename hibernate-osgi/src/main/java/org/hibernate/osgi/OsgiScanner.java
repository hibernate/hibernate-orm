/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import org.hibernate.boot.archive.scan.spi.AbstractScannerImpl;

import org.osgi.framework.Bundle;

/**
 * OSGi-specific implementation of the Scanner contract. Scans the persistence
 * unit Bundle for classes and resources.
 * 
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiScanner extends AbstractScannerImpl {
	/**
	 * Constructs the scanner for finding things in a OSGi bundle
	 *
	 * @param persistenceBundle The OSGi Bundle to scan
	 */
	public OsgiScanner(Bundle persistenceBundle) {
		super( new OsgiArchiveDescriptorFactory( persistenceBundle ) );
	}
}
