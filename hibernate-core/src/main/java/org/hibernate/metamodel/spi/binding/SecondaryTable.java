/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import org.hibernate.engine.FetchStyle;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 * @author Steve Ebersole
 */
public class SecondaryTable {
	private final TableSpecification secondaryTableReference;
	private final ForeignKey foreignKeyReference;
	private FetchStyle fetchStyle  = FetchStyle.JOIN;
	private boolean isInverse = false;
	private boolean isOptional = true;
	private boolean isCascadeDeleteEnabled;
	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	public SecondaryTable(TableSpecification secondaryTableReference, ForeignKey foreignKeyReference) {
		this.secondaryTableReference = secondaryTableReference;
		this.foreignKeyReference = foreignKeyReference;
	}

	public TableSpecification getSecondaryTableReference() {
		return secondaryTableReference;
	}

	public ForeignKey getForeignKeyReference() {
		return foreignKeyReference;
	}

	public FetchStyle getFetchStyle() {
		return fetchStyle;
	}

	public void setFetchStyle(FetchStyle fetchStyle) {
		this.fetchStyle = fetchStyle;
	}

	public boolean isInverse() {
		return isInverse;
	}

	public void setInverse(boolean isInverse) {
		this.isInverse = isInverse;
	}

	public boolean isOptional() {
		return isOptional;
	}

	public void setOptional(boolean isOptional) {
		this.isOptional = isOptional;
	}

	public boolean isCascadeDeleteEnabled() {
		return isCascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean isCascadeDeleteEnabled) {
		this.isCascadeDeleteEnabled = isCascadeDeleteEnabled;
	}
	public boolean isLazy() {
		// TODO: need to check attribute bindings using this table
		return false;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public void setCustomDelete(CustomSQL customDelete) {
		this.customDelete = customDelete;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public void setCustomInsert(CustomSQL customInsert) {
		this.customInsert = customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public void setCustomUpdate(CustomSQL customUpdate) {
		this.customUpdate = customUpdate;
	}
}
