/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.scan.spi.ScanningContext;
import org.hibernate.cfg.Environment;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class ScanningContextTestingImpl implements ScanningContext {
	public static final ScanningContextTestingImpl SCANNING_CONTEXT = new ScanningContextTestingImpl();

	private final Map<Object, Object> properties = Environment.getProperties();

	@Override
	public Map<Object, Object> getProperties() {
		return properties;
	}

	@Override
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return StandardArchiveDescriptorFactory.INSTANCE;
	}
}
