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

import java.net.URL;
import java.util.List;

/**
 * Describes the environment in which the scan will occur.
 * <p/>
 * Note that much of this comes from the PU in JPA sense.  This is
 * intended as an abstraction over the PU in JPA cases, as well as a
 * delegate allowing usage in non-JPA cases.  With the planned move
 * to unify the cfg.xml and persistence.xml schemas (like we are doing
 * with hbm.xml and orm.xml) this becomes less needed (at least parts
 * of it).
 * <p/>
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
	public URL getRootUrl();

	/**
	 * Returns any non-root URLs for scanning.  Can be null/empty to indicate
	 * that no non-root URL scanning should be done.
	 *
	 * @return The non-root URLs
	 *
	 * @see ScanOptions#canDetectUnlistedClassesInNonRoot()
	 */
	public List<URL> getNonRootUrls();

	/**
	 * Returns any classes which are explicitly listed as part of the
	 * "persistence unit".
	 *
	 * @return The explicitly listed classes
	 */
	public List<String> getExplicitlyListedClassNames();

	/**
	 * Returns the mapping files which are explicitly listed as part of the
	 * "persistence unit".
	 *
	 * @return The explicitly listed mapping files.
	 */
	public List<String> getExplicitlyListedMappingFiles();
}
