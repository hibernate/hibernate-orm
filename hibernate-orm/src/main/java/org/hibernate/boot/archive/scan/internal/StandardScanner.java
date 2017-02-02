/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.internal;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

/**
 * Standard implementation of the Scanner contract, supporting typical archive walking support where
 * the urls we are processing can be treated using normal file handling.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class StandardScanner extends AbstractScannerImpl {
	public StandardScanner() {
		this( StandardArchiveDescriptorFactory.INSTANCE );
	}

	public StandardScanner(ArchiveDescriptorFactory value) {
		super( value );
	}
}
