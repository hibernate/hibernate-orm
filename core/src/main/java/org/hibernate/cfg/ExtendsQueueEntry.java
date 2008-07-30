/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cfg;

import org.dom4j.Document;

/**
 * Represents a mapping queued for delayed processing to await
 * processing of an extends entity upon which it depends.
 *
 * @author Steve Ebersole
 */
public class ExtendsQueueEntry {
	private final String explicitName;
	private final String mappingPackage;
	private final Document document;

	public ExtendsQueueEntry(String explicitName, String mappingPackage, Document document) {
		this.explicitName = explicitName;
		this.mappingPackage = mappingPackage;
		this.document = document;
	}

	public String getExplicitName() {
		return explicitName;
	}

	public String getMappingPackage() {
		return mappingPackage;
	}

	public Document getDocument() {
		return document;
	}
}
