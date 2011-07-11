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
package org.hibernate.metamodel.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.Value;

/**
 * Convenient base class for {@link AttributeContainer}.  Because in our model all
 * {@link AttributeContainer AttributeContainers} are also {@link Hierarchical} we also implement that here
 * as well.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeContainer implements AttributeContainer, Hierarchical {
	private final String name;
	private final String className;
	private final Value<Class<?>> classReference;
	private final Hierarchical superType;
	private LinkedHashSet<Attribute> attributeSet = new LinkedHashSet<Attribute>();
	private HashMap<String, Attribute> attributeMap = new HashMap<String, Attribute>();

	public AbstractAttributeContainer(String name, String className, Value<Class<?>> classReference, Hierarchical superType) {
		this.name = name;
		this.className = className;
		this.classReference = classReference;
		this.superType = superType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public Class<?> getClassReference() {
		return classReference.getValue();
	}

	@Override
	public Value<Class<?>> getClassReferenceUnresolved() {
		return classReference;
	}

	@Override
	public Hierarchical getSuperType() {
		return superType;
	}

	@Override
	public Set<Attribute> getAttributes() {
		return Collections.unmodifiableSet( attributeSet );
	}

	@Override
	public Attribute getAttribute(String name) {
		return attributeMap.get( name );
	}

	@Override
	public SingularAttribute locateOrCreateSingularAttribute(String name) {
		SingularAttribute attribute = (SingularAttribute) getAttribute( name );
		if ( attribute == null ) {

			attribute = new SingularAttributeImpl( name, this );
			addAttribute( attribute );
		}
		return attribute;
	}

	@Override
	public SingularAttribute locateOrCreateComponentAttribute(String name) {
		SingularAttributeImpl attribute = (SingularAttributeImpl) getAttribute( name );
		if ( attribute == null ) {
			ComponentAttributeContainerDelegate component = new ComponentAttributeContainerDelegate();
			attribute = new SingularAttributeImpl( name, component );
			component.singularAttribute = attribute;
			addAttribute( attribute );
		}
		return attribute;
	}

	private static class ComponentAttributeContainerDelegate implements AttributeContainer {
		private SingularAttributeImpl singularAttribute;

		private ComponentAttributeContainerDelegate() {
		}

		private Component realComponent() {
			if ( singularAttribute.getSingularAttributeType() == null ) {
				throw new HibernateException( "Component type was not yet bound" );
			}
			if ( ! Component.class.isInstance( singularAttribute.getSingularAttributeType() ) ) {
				throw new HibernateException( "Unexpected bound type for component attribute" );
			}
			return (Component) singularAttribute.getSingularAttributeType();
		}

		@Override
		public Attribute getAttribute(String name) {
			return realComponent().getAttribute( name );
		}

		@Override
		public Set<Attribute> getAttributes() {
			return realComponent().getAttributes();
		}

		@Override
		public SingularAttribute locateOrCreateSingularAttribute(String name) {
			return realComponent().locateOrCreateSingularAttribute( name );
		}

		@Override
		public PluralAttribute locateOrCreatePluralAttribute(String name, PluralAttributeNature nature) {
			return realComponent().locateOrCreatePluralAttribute( name, nature );
		}

		@Override
		public PluralAttribute locateOrCreateBag(String name) {
			return realComponent().locateOrCreateBag( name );
		}

		@Override
		public PluralAttribute locateOrCreateSet(String name) {
			return realComponent().locateOrCreateBag( name );
		}

		@Override
		public IndexedPluralAttribute locateOrCreateList(String name) {
			return realComponent().locateOrCreateList( name );
		}

		@Override
		public IndexedPluralAttribute locateOrCreateMap(String name) {
			return realComponent().locateOrCreateMap( name );
		}

		@Override
		public SingularAttribute locateOrCreateComponentAttribute(String name) {
			return realComponent().locateOrCreateComponentAttribute( name );
		}

		@Override
		public String getName() {
			return realComponent().getName();
		}

		@Override
		public String getClassName() {
			return realComponent().getClassName();
		}

		@Override
		public Class<?> getClassReference() {
			return realComponent().getClassReference();
		}

		@Override
		public Value<Class<?>> getClassReferenceUnresolved() {
			return realComponent().getClassReferenceUnresolved();
		}

		@Override
		public boolean isAssociation() {
			return realComponent().isAssociation();
		}

		@Override
		public boolean isComponent() {
			return realComponent().isComponent();
		}
	}

	@Override
	public PluralAttribute locateOrCreateBag(String name) {
		return locateOrCreatePluralAttribute( name, PluralAttributeNature.BAG );
	}

	@Override
	public PluralAttribute locateOrCreateSet(String name) {
		return locateOrCreatePluralAttribute( name, PluralAttributeNature.SET );
	}

	@Override
	public IndexedPluralAttribute locateOrCreateList(String name) {
		return (IndexedPluralAttribute) locateOrCreatePluralAttribute( name, PluralAttributeNature.LIST );
	}

	@Override
	public IndexedPluralAttribute locateOrCreateMap(String name) {
		return (IndexedPluralAttribute) locateOrCreatePluralAttribute( name, PluralAttributeNature.MAP );
	}

	@Override
	public PluralAttribute locateOrCreatePluralAttribute(String name, PluralAttributeNature nature) {
		PluralAttribute attribute = (PluralAttribute) getAttribute( name );
		if ( attribute == null ) {
			attribute = nature.isIndexed()
					? new IndexedPluralAttributeImpl( name, nature, this )
					: new PluralAttributeImpl( name, nature, this );
			addAttribute( attribute );
		}
		return attribute;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AbstractAttributeContainer" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( ", superType=" ).append( superType );
		sb.append( '}' );
		return sb.toString();
	}

	protected void addAttribute(Attribute attribute) {
		// todo : how to best "secure" this?
		if ( attributeMap.put( attribute.getName(), attribute ) != null ) {
			throw new IllegalArgumentException( "Attribute with name [" + attribute.getName() + "] already registered" );
		}
		attributeSet.add( attribute );
	}

	// todo : inner classes for now..

	public static class SingularAttributeImpl implements SingularAttribute {
		private final AttributeContainer attributeContainer;
		private final String name;
		private Type type;

		public SingularAttributeImpl(String name, AttributeContainer attributeContainer) {
			this.name = name;
			this.attributeContainer = attributeContainer;
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
	}

	public static class PluralAttributeImpl implements PluralAttribute {
		private final AttributeContainer attributeContainer;
		private final PluralAttributeNature nature;
		private final String name;

		private Type elementType;

		public PluralAttributeImpl(String name, PluralAttributeNature nature, AttributeContainer attributeContainer) {
			this.name = name;
			this.nature = nature;
			this.attributeContainer = attributeContainer;
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
		public PluralAttributeNature getNature() {
			return nature;
		}

		@Override
		public String getName() {
			return name;
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

		public IndexedPluralAttributeImpl(String name, PluralAttributeNature nature, AttributeContainer attributeContainer) {
			super( name, nature, attributeContainer );
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