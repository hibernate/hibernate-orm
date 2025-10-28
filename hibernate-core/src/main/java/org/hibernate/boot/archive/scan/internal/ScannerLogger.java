/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import org.hibernate.boot.BootLogging;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;
import java.net.URL;

/**
 * Logging related to {@linkplain org.hibernate.boot.archive.scan.spi.Scanner scanning}.
 *
 * @author Gavin King
 */
@MessageLogger(projectCode = "HHH")
@SubSystemLogging(
		name = ScannerLogger.NAME,
		description = "Logging related to scanning"
)
@ValidIdRange(min = 60000, max = 60100)
public interface ScannerLogger extends BasicLogger {
	String NAME = BootLogging.NAME + ".scan";

	ScannerLogger SCANNER_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), ScannerLogger.class, NAME );

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = 60001, value = "Multiple ScannerFactory services available; using '%s'")
	void multipleScannerFactoriesAvailable(String scannerClassName);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = 60002, value = "No ScannerFactory available (to enable scanning add 'hibernate-scan-jandex' dependency or supply a custom ScannerFactory)")
	void noScannerFactoryAvailable();

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = 60003, value = "Unable to resolve class [%s] named in persistence unit [%s]")
	void unableToResolveClass(String className, URL rootUrl);
}
