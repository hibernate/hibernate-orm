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
package org.hibernate.metamodel.spi.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.spi.PluralAttributeNature;

/**
 * Convenient base class for {@link AttributeContainer}.  Because in our model all
 * {@link AttributeContainer AttributeContainers} are also {@link Hierarchical} we also implement that here
 * as well.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeContainer implements AttributeContainer, Hierarchical {
	private final JavaTypeDescriptor typeDescriptor;
	private final Hierarchical superType;

	private LinkedHashSet<Attribute> attributeSet = new LinkedHashSet<Attribute>();
	private HashMap<String, Attribute> attributeMap = new HashMap<String, Attribute>();

	public AbstractAttributeContainer(JavaTypeDescriptor typeDescriptor, Hierarchical superType) {
		this.typeDescriptor = typeDescriptor;
		this.superType = superType;
	}

	@Override
	public String getName() {
		return typeDescriptor.getName().toString();
	}

	@Override
	public String getRoleBaseName() {
		return getName();
	}

	@Override
	public JavaTypeDescriptor getDescriptor() {
		return typeDescriptor;
	}

	@Override
	public Hierarchical getSuperType() {
		return superType;
	}

	@Override
	public Set<Attribute> attributes() {
		return Collections.unmodifiableSet( attributeSet );
	}

	@Override
	public Attribute locateAttribute(String name) {
		return attributeMap.get( name );
	}

	@Override
	public SingularAttribute locateSingularAttribute(String name) {
		return (SingularAttribute) locateAttribute( name );
	}

	@Override
	public SingularAttribute locateCompositeAttribute(String name) {
		return (SingularAttributeImpl) locateAttribute( name );
	}

	@Override
	public PluralAttribute locatePluralAttribute(String name) {
		return (PluralAttribute) locateAttribute( name );
	}

	@Override
	public PluralAttribute locateBag(String name) {
		return locatePluralAttribute( name );
	}

	@Override
	public PluralAttribute locateSet(String name) {
		return locatePluralAttribute( name );
	}

	@Override
	public IndexedPluralAttribute locateList(String name) {
		return (IndexedPluralAttribute) locatePluralAttribute( name );
	}

	@Override
	public IndexedPluralAttribute locateMap(String name) {
		return (IndexedPluralAttribute) locatePluralAttribute( name );
	}

	@Override
	public String toString() {
		return "AbstractAttributeContainer" + "{name='" + getName() + "', superType=" + superType + '}';
	}


	@Override
	public SingularAttribute createSyntheticSingularAttribute(String name) {
		SingularAttribute attribute = new SingularAttributeImpl( this, name, true );
		addAttribute( attribute );
		return attribute;
	}

	@Override
	public SingularAttribute createSyntheticCompositeAttribute(String name, Hierarchical type) {
		SingularAttributeImpl attribute = new SingularAttributeImpl( this, name, true );
		attribute.resolveType( type );
		addAttribute( attribute );
		return attribute;
	}

	@Override
	public SingularAttribute createSingularAttribute(String name) {
		SingularAttribute attribute = new SingularAttributeImpl( this, name, false );
		addAttribute( attribute );
		return attribute;
	}

	@Override
	public SingularAttribute createCompositeAttribute(String name, Aggregate composite) {
		SingularAttributeImpl attribute = new SingularAttributeImpl( this, name, false );
		attribute.resolveType( composite );
		addAttribute( attribute );
		return attribute;
	}

	@Override
	public PluralAttribute createBag(String name) {
		return createPluralAttribute( name, PluralAttributeNature.BAG );
	}

	protected PluralAttribute createPluralAttribute(String name, PluralAttributeNature pluralAttributeNature) {
		PluralAttribute attribute = pluralAttributeNature.isIndexed()
				? new IndexedPluralAttributeImpl( this, name, pluralAttributeNature )
				: new PluralAttributeImpl( this, name, pluralAttributeNature );
		addAttribute( attribute );
		return attribute;
	}

	@Override
	public PluralAttribute createSet(String name) {
		return createPluralAttribute( name, PluralAttributeNature.SET );
	}

	@Override
	public IndexedPluralAttribute createList(String name) {
		return (IndexedPluralAttribute) createPluralAttribute( name, PluralAttributeNature.LIST );
	}

	@Override
	public IndexedPluralAttribute createArray(String name) {
		return (IndexedPluralAttribute) createPluralAttribute( name, PluralAttributeNature.ARRAY );
	}

	@Override
	public IndexedPluralAttribute createMap(String name) {
		return (IndexedPluralAttribute) createPluralAttribute( name, PluralAttributeNature.MAP );
	}

	protected void addAttribute(Attribute attribute) {
		if ( attributeMap.put( attribute.getName(), attribute ) != null ) {
			throw new IllegalArgumentException(
					"Attribute with name [" + attribute.getName() + "] already registered : " + getName()
			);
		}
		attributeSet.add( attribute );
	}

	// todo : inner classes for now..

	public static class SingularAttributeImpl implements SingularAttribute {
		private final AttributeContainer attributeContainer;
		private final String name;
		private final boolean synthetic;
		private Type type;

		public SingularAttributeImpl(AttributeContainer attributeContainer, String name, boolean synthetic) {
			this.attributeContainer = attributeContainer;
			this.name = name;
			this.synthetic = synthetic;
		}

		public boolean isTypeResolved() {
			return type != null;
		}

		public void resolveType(Type type) {
			if ( type == null ) {
				throw new IllegalArgumentException( "Attempt to resolve with null type" );
			}
			this.type = type;
		}

		@Override
		public Type getSingularAttributeType() {
			return type;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public AttributeContainer getAttributeContainer() {
			return attributeContainer;
		}

		@Override
		public boolean isSingular() {
			return true;
		}

		@Override
		public boolean isSynthetic() {
			return synthetic;
		}
	}

	public static class PluralAttributeImpl implements PluralAttribute {
		private final AttributeContainer attributeContainer;
		private final PluralAttributeNature pluralAttributeNature;
		private final String name;

		private Type elementType;

		public PluralAttributeImpl(AbstractAttributeContainer attributeContainer, String name, PluralAttributeNature pluralAttributeNature) {
			this.attributeContainer = attributeContainer;
			this.name = name;
			this.pluralAttributeNature = pluralAttributeNature;
		}

		@Override
		public AttributeContainer getAttributeContainer() {
			return attributeContainer;
		}

		@Override
		public boolean isSingular() {
			return false;
		}

		@Override
		public boolean isSynthetic() {
			// don't think there are ever any synthetic plural attributes created...
			return false;
		}

		@Override
		public PluralAttributeNature getPluralAttributeNature() {
			return pluralAttributeNature;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getRole() {
			return StringHelper.qualify( attributeContainer.getRoleBaseName(), name );
		}

		@Override
		public Type getElementType() {
			return elementType;
		}

		@Override
		public void setElementType(Type elementType) {
			this.elementType = elementType;
		}
	}

	public static class IndexedPluralAttributeImpl extends PluralAttributeImpl implements IndexedPluralAttribute {
		private Type indexType;

		public IndexedPluralAttributeImpl(AbstractAttributeContainer attributeContainer, String name, PluralAttributeNature pluralAttributeNature) {
			super( attributeContainer, name, pluralAttributeNature );
		}

		@Override
		public Type getIndexType() {
			return indexType;
		}

		@Override
		public void setIndexType(Type indexType) {
			this.indexType = indexType;
		}
	}
}