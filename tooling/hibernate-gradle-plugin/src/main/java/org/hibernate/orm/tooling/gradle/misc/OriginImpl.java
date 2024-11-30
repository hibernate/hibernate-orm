/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
