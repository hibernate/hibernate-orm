/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
