/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.scan.jandex;

import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningContext;
import org.hibernate.boot.scan.spi.ScanningProvider;
import org.jboss.jandex.IndexView;

/// Jandex-based implementation of ScannerProvider.
///
/// @author Steve Ebersole
public class ScanningProviderImpl implements ScanningProvider {
	public static final String JANDEX_INDEX = "hibernate.jandex.index";

	@Override
	public Scanner builderScanner(ScanningContext scanningContext) {
		var providedIndex = (IndexView) scanningContext.getProperties().get( JANDEX_INDEX );
		if ( providedIndex == null ) {
			return new IndexBuildingScanner( scanningContext );
		}
		else {
			return new ProvidedIndexScanner( scanningContext, providedIndex );
		}
	}
}
