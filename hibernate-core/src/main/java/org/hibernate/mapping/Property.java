/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * Represents a property as part of an entity or a component.
 *
 * @author Gavin King
 */
public class Property implements Serializable, MetaAttributable {
	private String name;
	private Value value;
	private String cascade;
	private boolean updateable = true;
	private boolean insertable = true;
	private boolean selectable = true;
	private boolean optimisticLocked = true;
	private ValueGeneration valueGenerationStrategy;
	private String propertyAccessorName;
	private boolean lazy;
	private String lazyGroup;
	private boolean optional;
	private java.util.Map metaAttributes;
	private PersistentClass persistentClass;
	private boolean naturalIdentifier;
	private boolean lob;

	public boolean isBackRef() {
		return false;
	}

	/**
	 * Does this property represent a synthetic property?  A synthetic property is one we create during
	 * metamodel binding to represent a collection of columns but which does not represent a property
	 * physically available on the entity.
	 *
	 * @return True if synthetic; false otherwise.
	 */
	public boolean isSynthetic() {
		return false;
	}

	public Type getType() throws MappingException {
		return value.getType();
	}
	
	public int getColumnSpan() {
		return value.getColumnSpan();
	}
	
	public Iterator getColumnIterator() {
		return value.getColumnIterator();
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isComposite() {
		return value instanceof Component;
	}

	public Value getValue() {
		return value;
	}
	
	public boolean isPrimitive(Class clazz) {
		return getGetter(clazz).getReturnType().isPrimitive();
	}

	public CascadeStyle getCascadeStyle() throws MappingException {
		Type type = value.getType();
		if ( type.isComponentType() ) {
			return getCompositeCascadeStyle( (CompositeType) type, cascade );
		}
		else if ( type.isCollectionType() ) {
			return getCollectionCascadeStyle( ( (Collection) value ).getElement().getType(), cascade );
		}
		else {
			return getCascadeStyle( cascade );			
		}
	}

	private static CascadeStyle getCompositeCascadeStyle(CompositeType compositeType, String cascade) {
		if ( compositeType.isAnyType() ) {
			return getCascadeStyle( cascade );
		}
		int length = compositeType.getSubtypes().length;
		for ( int i=0; i<length; i++ ) {
			if ( compositeType.getCascadeStyle(i) != CascadeStyles.NONE ) {
				return CascadeStyles.ALL;
			}
		}
		return getCascadeStyle( cascade );
	}

	private static CascadeStyle getCollectionCascadeStyle(Type elementType, String cascade) {
		if ( elementType.isComponentType() ) {
			return getCompositeCascadeStyle( (CompositeType) elementType, cascade );
		}
		else {
			return getCascadeStyle( cascade );
		}
	}
	
	private static CascadeStyle getCascadeStyle(String cascade) {
		if ( cascade==null || cascade.equals("none") ) {
			return CascadeStyles.NONE;
		}
		else {
			StringTokenizer tokens = new StringTokenizer(cascade, ", ");
			CascadeStyle[] styles = new CascadeStyle[ tokens.countTokens() ] ;
			int i=0;
			while ( tokens.hasMoreTokens() ) {
				styles[i++] = CascadeStyles.getCascadeStyle( tokens.nextToken() );
			}
			return new CascadeStyles.MultipleCascadeStyle(styles);
		}		
	}
	
	public String getCascade() {
		return cascade;
	}

	public void setCascade(String cascade) {
		this.cascade = cascade;
	}

	public void setName(String name) {
		this.name = name==null ? null : name.intern();
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public boolean isUpdateable() {
		// if the property mapping consists of all formulas,
		// make it non-updateable
		return updateable && !ArrayHelper.isAllFalse( value.getColumnUpdateability() );
	}

	public boolean isInsertable() {
		// if the property mapping consists of all formulas, 
		// make it non-insertable
		final boolean[] columnInsertability = value.getColumnInsertability();
		return insertable && (
				columnInsertability.length==0 ||
				!ArrayHelper.isAllFalse( columnInsertability )
			);
	}

	public ValueGeneration getValueGenerationStrategy() {
		return valueGenerationStrategy;
	}

	public void setValueGenerationStrategy(ValueGeneration valueGenerationStrategy) {
		this.valueGenerationStrategy = valueGenerationStrategy;
	}

	public void setUpdateable(boolean mutable) {
		this.updateable = mutable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	public void setPropertyAccessorName(String string) {
		propertyAccessorName = string;
	}

	/**
	 * Approximate!
	 */
	boolean isNullable() {
		return value==null || value.isNullable();
	}

	public boolean isBasicPropertyAccessor() {
		return propertyAccessorName==null || "property".equals( propertyAccessorName );
	}

	public java.util.Map getMetaAttributes() {
		return metaAttributes;
	}

	public MetaAttribute getMetaAttribute(String attributeName) {
		return metaAttributes==null?null:(MetaAttribute) metaAttributes.get(attributeName);
	}

	public void setMetaAttributes(java.util.Map metas) {
		this.metaAttributes = metas;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return getValue().isValid(mapping);
	}

	public String toString() {
		return getClass().getName() + '(' + name + ')';
	}
	
	public void setLazy(boolean lazy) {
		this.lazy=lazy;
	}
	
	public boolean isLazy() {
		if ( value instanceof ToOne ) {
			// both many-to-one and one-to-one are represented as a
			// Property.  EntityPersister is relying on this value to
			// determine "lazy fetch groups" in terms of field-level
			// interception.  So we need to make sure that we return
			// true here for the case of many-to-one and one-to-one
			// with lazy="no-proxy"
			//
			// * impl note - lazy="no-proxy" currently forces both
			// lazy and unwrap to be set to true.  The other case we
			// are extremely interested in here is that of lazy="proxy"
			// where lazy is set to true, but unwrap is set to false.
			// thus we use both here under the assumption that this
			// return is really only ever used during persister
			// construction to determine the lazy property/field fetch
			// groupings.  If that assertion changes then this check
			// needs to change as well.  Partially, this is an issue with
			// the overloading of the term "lazy" here...
			ToOne toOneValue = ( ToOne ) value;
			return toOneValue.isLazy() && toOneValue.isUnwrapProxy();
		}
		return lazy;
	}

	public String getLazyGroup() {
		return lazyGroup;
	}

	public void setLazyGroup(String lazyGroup) {
		this.lazyGroup = lazyGroup;
	}

	public boolean isOptimisticLocked() {
		return optimisticLocked;
	}

	public void setOptimisticLocked(boolean optimisticLocked) {
		this.optimisticLocked = optimisticLocked;
	}
	
	public boolean isOptional() {
		return optional || isNullable();
	}
	
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	public boolean isSelectable() {
		return selectable;
	}
	
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	public String getAccessorPropertyName( EntityMode mode ) {
		return getName();
	}

	// todo : remove
	public Getter getGetter(Class clazz) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessStrategy( clazz ).buildPropertyAccess( clazz, name ).getGetter();
	}

	// todo : remove
	public Setter getSetter(Class clazz) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessStrategy( clazz ).buildPropertyAccess( clazz, name ).getSetter();
	}

	// todo : remove
	public PropertyAccessStrategy getPropertyAccessStrategy(Class clazz) throws MappingException {
		String accessName = getPropertyAccessorName();
		if ( accessName == null ) {
			if ( clazz == null || java.util.Map.class.equals( clazz ) ) {
				accessName = "map";
			}
			else {
				accessName = "property";
			}
		}

		final EntityMode entityMode = clazz == null || java.util.Map.class.equals( clazz )
				? EntityMode.MAP
				: EntityMode.POJO;

		return resolveServiceRegistry().getService( PropertyAccessStrategyResolver.class ).resolvePropertyAccessStrategy(
				clazz,
				accessName,
				entityMode
		);
	}

	protected ServiceRegistry resolveServiceRegistry() {
		if ( getPersistentClass() != null ) {
			return getPersistentClass().getServiceRegistry();
		}
		if ( getValue() != null ) {
			return getValue().getServiceRegistry();
		}
		throw new HibernateException( "Could not resolve ServiceRegistry" );
	}

	public boolean isNaturalIdentifier() {
		return naturalIdentifier;
	}

	public void setNaturalIdentifier(boolean naturalIdentifier) {
		this.naturalIdentifier = naturalIdentifier;
	}

	public boolean isLob() {
		return lob;
	}

	public void setLob(boolean lob) {
		this.lob = lob;
	}

	public Property resolveConflict(PersistentClass owner, Property other) {
		if ( name.equals( other.getName() ) ) {
			if ( getValue() instanceof Component ) {
				// resolve conflicts in the components' properties that might happen due to usage of type variables
				return resolveComponentConflicts( (Component) getValue(), owner, (Component) other.getValue() );
			}

			// If we encounter that there are different types, then the property has it's type from a type variable
			if ( getValue() instanceof ToOne ) {
				ToOne value = (ToOne) getValue();
				ToOne otherValue = (ToOne) other.getValue();

				// If it's of the same type, everything is ok
				if ( value.getReferencedEntityName().equals( otherValue.getReferencedEntityName() ) ) {
					return this;
				}

				Object persistentOrMappedSuperclass = getCommonPersistentOrMappedSuperclass( other );
				String referencedEntityName = null;
				String typeName;

				if ( persistentOrMappedSuperclass instanceof PersistentClass ) {
					PersistentClass commonPersistentClass = (PersistentClass) persistentOrMappedSuperclass;

					// Update this property and ToOne to that common type
					// This property is the one of the super type, it has to have the common super type of all subtype properties
					value.setTypeName( commonPersistentClass.getEntityName() );
					value.setReferencedEntityName( commonPersistentClass.getEntityName() );

					if ( !isMappedSuperclassDeclaredProperty( owner, other ) ) {
						return this;
					}
					referencedEntityName = commonPersistentClass.getEntityName();
					typeName = referencedEntityName;
				} else {
					typeName = ((MappedSuperclass) persistentOrMappedSuperclass).getMappedClass().getName();
				}

				MetadataImplementor metadata = otherValue.getMetadata();

				// Since there is no entity available we have to construct a virtual property
				Property resolvedProperty = new Property();

				ToOne resolvedValue;

				if ( otherValue instanceof ManyToOne ) {
					ManyToOne manyToOne = (ManyToOne) otherValue;
					ManyToOne newManyToOne = new ManyToOne( metadata, otherValue.getTable() );
					newManyToOne.setIgnoreNotFound( manyToOne.isIgnoreNotFound() );
					if ( manyToOne.isLogicalOneToOne() ) {
						newManyToOne.markAsLogicalOneToOne();
					}
					resolvedValue = newManyToOne;
				} else if ( otherValue instanceof OneToOne ) {
					OneToOne oneToOne = (OneToOne) otherValue;
					Class<?> ownerClass = metadata.getTypeResolver().heuristicType( oneToOne.getEntityName() ).getReturnedClass();
					PersistentClass oneToOneOwner = findPersistentClass( metadata, ownerClass );
					OneToOne newOneToOne = new OneToOne( metadata, otherValue.getTable(), oneToOneOwner );
					newOneToOne.setReferencedPropertyName( oneToOne.getReferencedPropertyName());
					newOneToOne.setForeignKeyType( oneToOne.getForeignKeyType() );
					newOneToOne.setConstrained( oneToOne.isConstrained() );
					resolvedValue = newOneToOne;
				} else {
					throw new UnsupportedOperationException( "No type conflict resolution implemented for ToOne type: " + otherValue );
				}

				// Copy ToOne state
				resolvedValue.setFetchMode( otherValue.getFetchMode() );
				resolvedValue.setReferencedPropertyName( otherValue.getReferencedPropertyName() );
				resolvedValue.setReferencedEntityName( referencedEntityName );
				resolvedValue.setLazy( otherValue.isLazy() );
				resolvedValue.setUnwrapProxy( otherValue.isUnwrapProxy() );
				resolvedValue.setReferenceToPrimaryKey( otherValue.isReferenceToPrimaryKey() );

				// Create fake columns to by-pass mapping check
				KeyValue referencedValue = otherValue.getTable().getIdentifierValue();
				boolean[] insertability = referencedValue.getColumnInsertability();
				boolean[] updateability = referencedValue.getColumnUpdateability();
				Iterator<Selectable> iter = referencedValue.getColumnIterator();
				int index = 0;

				while ( iter.hasNext() ) {
					Selectable selectable = iter.next();
					if ( selectable instanceof Formula ) {
						resolvedValue.addFormula( (Formula) selectable );
					} else {
						resolvedValue.addColumn( (Column) selectable, insertability[index], updateability[index] );
					}

					index++;
				}

				// Copy SimpleValue state
				resolvedValue.setTypeName( typeName );
				resolvedValue.setTypeParameters( otherValue.getTypeParameters() );

				if ( otherValue.isVersion() ) {
					resolvedValue.makeVersion();
				}
				if ( otherValue.isNationalized() ) {
					resolvedValue.makeNationalized();
				}
				if ( otherValue.isLob() ) {
					resolvedValue.makeLob();
				}

				resolvedValue.setIdentifierGeneratorProperties( otherValue.getIdentifierGeneratorProperties() );
				resolvedValue.setIdentifierGeneratorStrategy( otherValue.getIdentifierGeneratorStrategy() );
				resolvedValue.setNullValue( otherValue.getNullValue() );
				// This might be wrong?
				resolvedValue.setTable( otherValue.getTable() );
				resolvedValue.setForeignKeyName( otherValue.getForeignKeyName() );
				resolvedValue.setForeignKeyDefinition( otherValue.getForeignKeyDefinition() );
				resolvedValue.setAlternateUniqueKey( otherValue.isAlternateUniqueKey() );
				resolvedValue.setCascadeDeleteEnabled( otherValue.isCascadeDeleteEnabled() );
				resolvedValue.copyTypeFrom( otherValue );

				resolvedProperty.name = name;
				resolvedProperty.value = resolvedValue;
				resolvedProperty.cascade = other.cascade;
				resolvedProperty.updateable = other.updateable;
				resolvedProperty.insertable = other.insertable;
				resolvedProperty.selectable = other.selectable;
				resolvedProperty.optimisticLocked = other.optimisticLocked;
				resolvedProperty.valueGenerationStrategy = other.valueGenerationStrategy;
				resolvedProperty.propertyAccessorName = other.propertyAccessorName;
				resolvedProperty.lazy = other.lazy;
				resolvedProperty.lazyGroup = other.lazyGroup;
				resolvedProperty.optional = other.optional;
				resolvedProperty.metaAttributes = other.metaAttributes;
				resolvedProperty.persistentClass = other.persistentClass;
				resolvedProperty.naturalIdentifier = other.naturalIdentifier;
				resolvedProperty.lob = other.lob;

				return resolvedProperty;
			}

			if ( getValue() instanceof SimpleValue ) {
				String typeName = ( (SimpleValue) getValue() ).getTypeName();
				String otherTypeName = ( (SimpleValue) other.getValue() ).getTypeName();
				// For some simple property values the types might not be set yet
				if ( typeName == null || otherTypeName == null || typeName.equals( otherTypeName ) ) {
					return this;
				}
			}

			if ( getValue() instanceof Collection ) {
				return this;
			}

			// If it's of the same type, everything is ok
			if ( getType().equals( other.getType() ) ) {
				return this;
			}
		}

		return null;
	}

	private static boolean isMappedSuperclassDeclaredProperty(PersistentClass persistentClass, Property property) {
		MappedSuperclass mappedSuperclass = persistentClass.getSuperMappedSuperclass();

		// Go up the mapped super class hierarchy and check if the property is declared there
		while ( mappedSuperclass != null ) {
			Iterator<Property> declaredPropertiesIter = mappedSuperclass.getDeclaredPropertyIterator();
			while ( declaredPropertiesIter.hasNext() ) {
				if ( declaredPropertiesIter.next() == property ) {
					return true;
				}
			}

			mappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		}

		return false;
	}

	private PersistentClass resolveReferencedPersistentClass() {
		ToOne value = (ToOne) getValue();
		MetadataImplementor metadata = value.getMetadata();
		PersistentClass candidate = metadata.getEntityBinding( value.getReferencedEntityName() );
		if ( candidate != null ) {
			return candidate;
		}
		return null;
	}

	private Property resolveComponentConflicts(Component comp1, PersistentClass comp2Owner, Component comp2) {
		Iterator it1 = comp1.getPropertyIterator();
		while ( it1.hasNext() ) {
			final Property prop = (Property) it1.next();
			String name = prop.getName();
			Iterator it2 = comp2.getPropertyIterator();
			while ( it2.hasNext() ) {
				final Property other = (Property) it2.next();
				if ( name.equals( other.getName() ) ) {
					if ( prop.resolveConflict( comp2Owner, other ) != null ) {
						break;
					}

					return null;
				}
			}
		}

		return this;
	}

	private Object getCommonPersistentOrMappedSuperclass(Property property) {
		PersistentClass persistentClass1 = resolveReferencedPersistentClass();
		PersistentClass persistentClass2 = property.resolveReferencedPersistentClass();

		if ( persistentClass1 != null && persistentClass2 != null ) {
			return getCommonPersistentClass( persistentClass1, persistentClass2 );
		}

		Class<?> class1 = resolveType( (ToOne) getValue() ).getReturnedClass();
		Class<?> class2 = resolveType( (ToOne) property.getValue() ).getReturnedClass();
		Class<?> commonClass = getCommonPersistentClass( class1, class2 );

		MetadataImplementor metadata = ( (ToOne) getValue() ).getMetadata();
		PersistentClass candidate = findPersistentClass( metadata, commonClass );
		if ( candidate != null ) {
			return candidate;
		}

		MappedSuperclass mappedSuperclass = null;

		if ( metadata instanceof InFlightMetadataCollector ) {
			mappedSuperclass = ( (InFlightMetadataCollector) metadata ).getMappedSuperclass( commonClass );
		}

		if ( metadata == null ) {
			for ( MappedSuperclass clazz : metadata.getMappedSuperclassMappingsCopy() ) {
				if ( clazz.getMappedClass() == commonClass ) {
					mappedSuperclass = clazz;
					break;
				}
			}
		}

		if ( mappedSuperclass != null ) {
			return mappedSuperclass;
		}

		throw new IllegalStateException( "Could not find the PersistentClass for: " + commonClass.getName() );
	}

	private Type resolveType(ToOne value) {
		if ( value.getType() != null ) {
			return value.getType();
		}
		MetadataImplementor metadata = value.getMetadata();
		return metadata.getTypeResolver().heuristicType( value.getTypeName() );
	}

	private PersistentClass getCommonPersistentClass(PersistentClass clazz1, PersistentClass clazz2) {
		while ( !clazz2.getMappedClass().isAssignableFrom( clazz1.getMappedClass() ) ) {
			clazz2 = clazz2.getSuperclass();
		}
		return clazz2;
	}

	private Class<?> getCommonPersistentClass(Class<?> clazz1, Class<?> clazz2) {
		while ( !clazz2.isAssignableFrom( clazz1 ) ) {
			clazz2 = clazz2.getSuperclass();
		}
		return clazz2;
	}

	private PersistentClass findPersistentClass(MetadataImplementor metadata, Class<?> clazz) {
		PersistentClass newClass = null;

		// Go up the class hierarchy
		while ( newClass == null && clazz != Object.class ) {
			// Iterate through entity bindings and match by class objects
			for ( PersistentClass persistentClass : metadata.getEntityBindings() ) {
				// Stop when we found a class that has a matching class object
				if ( clazz == persistentClass.getMappedClass() ) {
					return persistentClass;
				}
			}

			clazz = clazz.getSuperclass();
		}

		return null;
	}

}
