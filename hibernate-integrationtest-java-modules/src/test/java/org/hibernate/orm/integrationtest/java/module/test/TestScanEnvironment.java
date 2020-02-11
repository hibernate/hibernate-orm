/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.integrationtest.java.module.test;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.archive.scan.spi.ScanEnvironment;

final class TestScanEnvironment implements ScanEnvironment {

	private final URL root;

	TestScanEnvironment(URL root) {
		this.root = root;
	}

	@Override
	public URL getRootUrl() {
		return root;
	}

	@Override
	public List<URL> getNonRootUrls() {
		return Collections.emptyList();
	}

	@Override
	public List<String> getExplicitlyListedClassNames() {
		return Collections.emptyList();
	}

	@Override
	public List<String> getExplicitlyListedMappingFiles() {
		return Collections.emptyList();
	}
}
