/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.factories.components.ModuleMetadataFileFinder;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class SecondLevelCacheMetadataModuleFinder implements ModuleMetadataFileFinder {
	@Override
	public String getMetadataFilename() {
		return "hibernate-infinispan-component-metadata.dat";
	}
}
