/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;


import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.Origin;

import javax.xml.stream.XMLEventReader;

/**
 * @author Jan Schatteman
 */
public interface JaxbBindingSource {
	Origin getOrigin();
	InputStreamAccess getInputStreamAccess();
	XMLEventReader getEventReader();
}
