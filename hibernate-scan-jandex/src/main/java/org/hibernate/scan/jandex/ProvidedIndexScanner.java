/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.scan.jandex;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.scan.internal.ResultCollector;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningContext;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.jboss.jandex.IndexView;

import java.net.URL;

/**
 * @author Steve Ebersole
 */
public class ProvidedIndexScanner implements Scanner {
	private final ScanningContext scanningContext;
	private final IndexView jandexIndex;

	public ProvidedIndexScanner(ScanningContext scanningContext, IndexView existingJandexIndex) {
		this.scanningContext = scanningContext;
		this.jandexIndex = existingJandexIndex;
		assert existingJandexIndex != null;
	}

	@Override
	public ScanningResult scan(URL... boundaries) {
		var resultCollector = new ResultCollector();
		IndexScanner.scanForClasses( jandexIndex, resultCollector );
		return resultCollector.toResult();
	}

	@Override
	public ScanningResult jpaScan(ArchiveDescriptor rootArchive, JaxbPersistenceImpl.JaxbPersistenceUnitImpl jaxbUnit) {
		// todo (jpa4) : exclude-unlisted-classes poses a problem with an existing Jandex
		//		in that there is no distinction in the Jandex about "source"[1] - we'd either need to
		//		skip or do discovery across all archives
		// [1] there "might be" in that a CompositeIndex is used under the covers, thought not sure we can properly leverage that here.
		var resultCollector = new ResultCollector();
		for ( String jarFileRef : jaxbUnit.getJarFiles() ) {
			final var jarArchive = rootArchive.resolveJarFileReference( jarFileRef );
		}
		IndexScanner.scanForClasses( jandexIndex, resultCollector );
		return resultCollector.toResult();
	}
}
