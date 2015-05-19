/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.spi;

/**
 * Defines the contract for Hibernate to be able to scan for classes, packages and resources inside a
 * persistence unit.
 * <p/>
 * Constructors are expected in one of 2 forms:<ul>
 *     <li>no-arg</li>
 *     <li>single arg, of type {@link org.hibernate.boot.archive.spi.ArchiveDescriptorFactory}</li>
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
