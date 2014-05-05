/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.archive.scan.spi;

/**
 * Defines the contract for Hibernate to be able to scan for classes, packages and resources inside a
 * persistence unit.
 * <p/>
 * Constructors are expected in one of 2 forms:<ul>
 *     <li>no-arg</li>
 *     <li>single arg, of type {@link org.hibernate.metamodel.archive.spi.ArchiveDescriptorFactory}</li>
 * </ul>
 * <p/>
 * If a ArchiveDescriptorFactory is specified in the configuration, but the Scanner
 * to be used does not accept a ArchiveDescriptorFactory an exception will be thrown.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface Scanner {
	/**
	 * Perform the scanning against the described environment using the
	 * defined options, and return the scan results.
	 *
	 * @param environment The scan environment.
	 * @param options The options to control the scanning.
	 * @param params The parameters for scanning
	 */
	public ScanResult scan(ScanEnvironment environment, ScanOptions options, ScanParameters params);
}
