/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.spi;

import java.net.URL;
import java.util.List;

/**
 * Describes the environment in which the scan will occur.
 * <p>
 * Note that much of this comes from the PU in JPA sense.  This is
 * intended as an abstraction over the PU in JPA cases, as well as a
 * delegate allowing usage in non-JPA cases.  With the planned move
 * to unify the cfg.xml and persistence.xml schemas (like we are doing
 * with hbm.xml and orm.xml) this becomes less needed (at least parts
 * of it).
 * <p>
 * After unification, I think the biggest difference is that we will
 * not need to pass ScanEnvironment into the MetadataSources/MetadataBuilder
 * while for the time being we will need to.
 *
 * @author Steve Ebersole
 */
public interface ScanEnvironment {
	/**
	 * Returns the root URL for scanning.  Can be {@code null}, indicating that
	 * no root URL scanning should be done (aka, if maybe a root URL is not known).
	 *
	 * @return The root URL
	 *
	 * @see ScanOptions#canDetectUnlistedClassesInRoot()
	 */
	URL getRootUrl();

	/**
	 * Returns any non-root URLs for scanning.  Can be null/empty to indicate
	 * that no non-root URL scanning should be done.
	 *
	 * @return The non-root URLs
	 *
	 * @see ScanOptions#canDetectUnlistedClassesInNonRoot()
	 */
	List<URL> getNonRootUrls();

	/**
	 * Returns any classes which are explicitly listed as part of the
	 * "persistence unit".
	 *
	 * @return The explicitly listed classes
	 */
	List<String> getExplicitlyListedClassNames();

	/**
	 * Returns the mapping files which are explicitly listed as part of the
	 * "persistence unit".
	 *
	 * @return The explicitly listed mapping files.
	 */
	List<String> getExplicitlyListedMappingFiles();
}
