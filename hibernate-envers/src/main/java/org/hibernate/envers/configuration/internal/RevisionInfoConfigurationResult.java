/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
