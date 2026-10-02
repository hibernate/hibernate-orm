package org.hibernate.boot.scan.spi;

import org.hibernate.service.JavaServiceLoadable;


/// Provider for [Scanner] instances.
///
/// @author Steve Ebersole
@JavaServiceLoadable
public interface ScanningProvider {
	/// Create a scanner.
	///
	/// @param scanningContext The context of the scan, providing access to useful information.
	Scanner builderScanner(ScanningContext scanningContext);
}
