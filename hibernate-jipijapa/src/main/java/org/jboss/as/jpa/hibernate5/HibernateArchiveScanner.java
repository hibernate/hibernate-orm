/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.jboss.as.jpa.hibernate5;


import org.hibernate.boot.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.boot.archive.scan.spi.Scanner;

/**
 * Annotation scanner for Hibernate.  Essentially just passes along the VFS-based ArchiveDescriptorFactory
 *
 * @author Steve Ebersole
 */
public class HibernateArchiveScanner extends AbstractScannerImpl implements Scanner {
	public HibernateArchiveScanner() {
		super( VirtualFileSystemArchiveDescriptorFactory.INSTANCE );
	}
}
