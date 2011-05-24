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
package org.hibernate.metamodel.binding;

/**
 * @author Gail Badner
 */
public enum CollectionElementType {
	ELEMENT( "element" ) {
		public CollectionElement createCollectionElementInternal(PluralAttributeBinding attributeBinding) {
			return new ElementCollectionElement( attributeBinding  );
		}
	},
	COMPOSITE_ELEMENT( "composite-element" ) {
		public CollectionElement createCollectionElementInternal(PluralAttributeBinding attributeBinding) {
			return new ElementCollectionElement( attributeBinding  );
		}
	},
	ONE_TO_MANY( "one-to-many" ) {
		public boolean isOneToMany() {
			return true;
		}
		public CollectionElement createCollectionElementInternal(PluralAttributeBinding attributeBinding) {
			return new OneToManyCollectionElement( attributeBinding  );
		}
	},
	MANY_TO_MANY( "many-to-many" ) {
		public boolean isManyToMany() {
			return true;
		}
		public CollectionElement createCollectionElementInternal(PluralAttributeBinding attributeBinding) {
			return new ManyToManyCollectionElement( attributeBinding  );
		}
	},
	MANY_TO_ANY( "many-to-any" ) {
		//TODO: should isManyToMany() return true?
		public boolean isManyToAny() {
			return true;
		}
		public CollectionElement createCollectionElementInternal(PluralAttributeBinding attributeBinding) {
			return new ManyToAnyCollectionElement( attributeBinding  );
		}
	};

	private final String name;

	private CollectionElementType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return super.toString() + "[" + getName() + "]";
	}

	public boolean isOneToMany() {
		return false;
	}

	public boolean isManyToMany() {
		return false;
	}

	public boolean isManyToAny() {
		return false;
	}

	protected abstract CollectionElement createCollectionElementInternal(PluralAttributeBinding attributeBinding);

	/* package-protected */
	 CollectionElement createCollectionElement(PluralAttributeBinding attributeBinding) {
		 CollectionElement collectionElement = createCollectionElementInternal( attributeBinding );
		 if ( collectionElement.getCollectionElementType() != this ) {
			 throw new IllegalStateException( "Collection element has unexpected type nature: actual=[" +
			 collectionElement.getCollectionElementType() + "; expected=[" + this + "]" );
		 }
		 return collectionElement;
	 }
}

