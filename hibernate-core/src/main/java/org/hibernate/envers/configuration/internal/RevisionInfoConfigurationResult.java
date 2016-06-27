/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class RevisionInfoConfigurationResult {
	private final RevisionInfoGenerator revisionInfoGenerator;
	private final Document revisionInfoXmlMapping;
	private final RevisionInfoQueryCreator revisionInfoQueryCreator;
	private final Element revisionInfoRelationMapping;
	private final RevisionInfoNumberReader revisionInfoNumberReader;
	private final ModifiedEntityNamesReader modifiedEntityNamesReader;
	private final String revisionInfoEntityName;
	private final Class<?> revisionInfoClass;
	private final PropertyData revisionInfoTimestampData;

	RevisionInfoConfigurationResult(
			RevisionInfoGenerator revisionInfoGenerator,
			Document revisionInfoXmlMapping, RevisionInfoQueryCreator revisionInfoQueryCreator,
			Element revisionInfoRelationMapping, RevisionInfoNumberReader revisionInfoNumberReader,
			ModifiedEntityNamesReader modifiedEntityNamesReader, String revisionInfoEntityName,
			Class<?> revisionInfoClass, PropertyData revisionInfoTimestampData) {
		this.revisionInfoGenerator = revisionInfoGenerator;
		this.revisionInfoXmlMapping = revisionInfoXmlMapping;
		this.revisionInfoQueryCreator = revisionInfoQueryCreator;
		this.revisionInfoRelationMapping = revisionInfoRelationMapping;
		this.revisionInfoNumberReader = revisionInfoNumberReader;
		this.modifiedEntityNamesReader = modifiedEntityNamesReader;
		this.revisionInfoEntityName = revisionInfoEntityName;
		this.revisionInfoClass = revisionInfoClass;
		this.revisionInfoTimestampData = revisionInfoTimestampData;
	}

	public RevisionInfoGenerator getRevisionInfoGenerator() {
		return revisionInfoGenerator;
	}

	public Document getRevisionInfoXmlMapping() {
		return revisionInfoXmlMapping;
	}

	public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
		return revisionInfoQueryCreator;
	}

	public Element getRevisionInfoRelationMapping() {
		return revisionInfoRelationMapping;
	}

	public RevisionInfoNumberReader getRevisionInfoNumberReader() {
		return revisionInfoNumberReader;
	}

	public String getRevisionInfoEntityName() {
		return revisionInfoEntityName;
	}

	public Class<?> getRevisionInfoClass() {
		return revisionInfoClass;
	}

	public PropertyData getRevisionInfoTimestampData() {
		return revisionInfoTimestampData;
	}

	public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
		return modifiedEntityNamesReader;
	}
}
