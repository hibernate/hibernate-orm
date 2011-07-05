/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binder.source.annotations;

import org.hibernate.metamodel.binder.Origin;
import org.hibernate.metamodel.binder.source.EntityDescriptor;
import org.hibernate.metamodel.binder.source.MetaAttributeContext;
import org.hibernate.metamodel.binder.source.TableDescriptor;
import org.hibernate.metamodel.binder.source.UnifiedDescriptorObject;

/**
 * @author Steve Ebersole
 */
public class TableDescriptorImpl implements TableDescriptor {
	private final String explicitSchemaName;
	private final String explicitCatalogName;
	private final String tableName;

	private final EntityDescriptor entityDescriptor;
	private final AnnotationsBindingContext bindingContext;

	public TableDescriptorImpl(
			String explicitSchemaName,
			String explicitCatalogName,
			String tableName,
			EntityDescriptor entityDescriptor, AnnotationsBindingContext bindingContext) {
		this.explicitSchemaName = explicitSchemaName;
		this.explicitCatalogName = explicitCatalogName;
		this.tableName = tableName;
		this.entityDescriptor = entityDescriptor;
		this.bindingContext = bindingContext;
	}

	@Override
	public String getExplicitSchemaName() {
		return explicitSchemaName;
	}

	@Override
	public String getExplicitCatalogName() {
		return explicitCatalogName;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public Origin getOrigin() {
//		return bindingContext.getOrigin();
		return null;
	}

	@Override
	public UnifiedDescriptorObject getContainingDescriptor() {
		return entityDescriptor;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return null;
	}
}
