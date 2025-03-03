/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.misc;

import java.io.File;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;

/**
 * @author Steve Ebersole
 */
public class OriginImpl extends Origin {
	private final File hbmXmlFile;

	public OriginImpl(File hbmXmlFile) {
		super( SourceType.FILE, hbmXmlFile.getAbsolutePath() );
		this.hbmXmlFile = hbmXmlFile;
	}

	public File getHbmXmlFile() {
		return hbmXmlFile;
	}
}
