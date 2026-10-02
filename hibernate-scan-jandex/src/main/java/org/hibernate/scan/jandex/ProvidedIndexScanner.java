package org.hibernate.scan.jandex;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
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
		// An IndexView has no archive provenance. Use it for annotation definitions,
		// but derive candidates from the requested archives.
		return new IndexBuildingScanner( scanningContext, jandexIndex ).scan( boundaries );
	}

	@Override
	public ScanningResult jpaScan(ArchiveDescriptor rootArchive, JaxbPersistenceImpl.JaxbPersistenceUnitImpl jaxbUnit) {
		return new IndexBuildingScanner( scanningContext, jandexIndex ).jpaScan( rootArchive, jaxbUnit );
	}
}
