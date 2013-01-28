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
package org.hibernate.mapping;
import org.hibernate.MappingException;
import org.hibernate.cfg.Mappings;
import org.hibernate.type.CollectionType;

/**
 * A list mapping has a primary key consisting of the key columns + index column.
 *
 * @author Gavin King
 */
public class List extends IndexedCollection {
	
	private int baseIndex;

	public boolean isList() {
		return true;
	}

	public List(Mappings mappings, PersistentClass owner) {
		super( mappings, owner );
	}

	public CollectionType getDefaultCollectionType() throws MappingException {
		return getMappings().getTypeResolver()
				.getTypeFactory()
				.list( getRole(), getReferencedPropertyName() );
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public int getBaseIndex() {
		return baseIndex;
	}
	
	public void setBaseIndex(int baseIndex) {
		this.baseIndex = baseIndex;
	}
}
