/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import org.hibernate.metamodel.relational.Value;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class CollectionElement {

	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final PluralAttributeBinding collectionBinding;
	private final CollectionElementType collectionElementType;
	private String nodeName;

	private Value elementValue;

	CollectionElement(PluralAttributeBinding collectionBinding, CollectionElementType collectionElementType) {
		this.collectionBinding = collectionBinding;
		this.collectionElementType = collectionElementType;
	}

	public final CollectionElementType getCollectionElementType() {
		return collectionElementType;
	}

	/* package-protected */
	void setTypeName(String typeName) {
		hibernateTypeDescriptor.setExplicitTypeName( typeName );
	}

	/* package-protected */
	void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public boolean isOneToMany() {
		return collectionElementType.isOneToMany();
	}

	public boolean isManyToMany() {
		return collectionElementType.isManyToMany();
	}

}
