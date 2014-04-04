/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.metamodel.source.internal.jandex;

import java.io.Serializable;

import javax.persistence.AccessType;

public class Default implements Serializable {
	private AccessType access;
	private String packageName;
	private String schema;
	private String catalog;
	private Boolean metadataComplete;
	private Boolean cascadePersist;

	public AccessType getAccess() {
		return access;
	}

	public void setAccess(AccessType access) {
		this.access = access;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public Boolean isMetadataComplete() {
		return metadataComplete;
	}

	public void setMetadataComplete(Boolean metadataComplete) {
		this.metadataComplete = metadataComplete;
	}

	public Boolean isCascadePersist() {
		return cascadePersist;
	}

	public void setCascadePersist(Boolean cascadePersist) {
		this.cascadePersist = cascadePersist;
	}

	void override(Default globalDefault) {
		if ( globalDefault != null ) {
			if ( globalDefault.getAccess() != null ) {
				access = globalDefault.getAccess();
			}
			if ( globalDefault.getPackageName() != null ) {
				packageName = globalDefault.getPackageName();
			}
			if ( globalDefault.getSchema() != null ) {
				schema = globalDefault.getSchema();
			}
			if ( globalDefault.getCatalog() != null ) {
				catalog = globalDefault.getCatalog();
			}
			if ( globalDefault.isCascadePersist() != null ) {
				cascadePersist = globalDefault.isCascadePersist();
			}
			if ( globalDefault.isMetadataComplete() != null ) {
				metadataComplete = globalDefault.isMetadataComplete();
			}

		}
	}
}
