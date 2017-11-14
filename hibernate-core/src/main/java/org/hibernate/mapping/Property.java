/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.cfg.Environment;
import org.hibernate.collection.spi.PersistentCollectionRepresentation;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.internal.BasicSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.SingularAttributeClassification;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.ValueGeneration;

/**
 * Represents a property as part of an entity or a component.
 *
 * @author Gavin King
 */
public class Property implements Serializable, PersistentAttributeMapping {
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

	@Override
	public ValueMapping getValueMapping() {
		return value;
	}

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


	
	public int getColumnSpan() {
		return value.getColumnSpan();
	}

	/**
	 * @deprecated since 6.0, use {@link #getMappedColumns()}
	 */
	@Deprecated
	public Iterator getColumnIterator() {
		return value.getColumnIterator();
	}

	public List<MappedColumn> getMappedColumns(){
		return value.getMappedColumns();
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
//
//	public CascadeStyle getCascadeStyle() throws MappingException {
//		Type type = value.getType();
//		if ( type.isComponentType() ) {
//			return getCompositeCascadeStyle( (EmbeddedType) type, cascade );
//		}
//		else if ( type.getClassification().equals( Type.Classification.COLLECTION ) ) {
//			return getCollectionCascadeStyle( ( (Collection) value ).getElement().getType(), cascade );
//		}
//		else {
//			return getCascadeStyle( cascade );
//		}
//	}
//
//	private static CascadeStyle getCompositeCascadeStyle(EmbeddedType compositeType, String cascade) {
//		if ( compositeType.getClassification().equals( Type.Classification.ANY ) ) {
//			return getCascadeStyle( cascade );
//		}
//		int length = compositeType.getSubtypes().length;
//		for ( int i=0; i<length; i++ ) {
//			if ( compositeType.getCascadeStyle(i) != CascadeStyles.NONE ) {
//				return CascadeStyles.ALL;
//			}
//		}
//		return getCascadeStyle( cascade );
//	}
//
//	private static CascadeStyle getCollectionCascadeStyle(Type elementType, String cascade) {
//		if ( elementType.isComponentType() ) {
//			return getCompositeCascadeStyle( (EmbeddedType) elementType, cascade );
//		}
//		else {
//			return getCascadeStyle( cascade );
//		}
//	}
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

	@Override
	public boolean isIncludedInDirtyChecking() {
		return isUpdateable() || shouldAlwaysDirtyCheck();
	}

	private boolean shouldAlwaysDirtyCheck() {
		// todo (6.0) : verify this is correct
		//		it matches what 5.2 does for sure
		return value instanceof Collection || value instanceof ManyToOne;
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

	public boolean isValid() throws MappingException {
		return getValue().isValid();
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

	@Override
	public EntityMapping getEntity(){
		return getEntity();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return optimisticLocked;
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
		if ( persistentClass != null ) {
			return persistentClass.getServiceRegistry();
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

	@Override
	public <O,T> PersistentAttribute<O,T> makeRuntimeAttribute(
			ManagedTypeDescriptor<O> runtimeContainer,
			ManagedTypeMapping bootContainer,
			SingularPersistentAttribute.Disposition singularAttributeDisposition,
			RuntimeModelCreationContext context) {
		assert value != null;

		// todo (7.0) : better served through polymorphism

		// todo (6.0) : how to handle synthetic/virtual properties?
		assert !Backref.class.isInstance( this );
		assert !IndexBackref.class.isInstance( this );
		assert !SyntheticProperty.class.isInstance( this );

		if ( value instanceof Collection ) {
			return buildCollectionAttribute( runtimeContainer, bootContainer, context );
		}
		else {
			return buildSingularAttribute( runtimeContainer, bootContainer, singularAttributeDisposition, context );
		}
	}
	@SuppressWarnings("unchecked")
	private <O,T> PluralPersistentAttribute<O,T,?> buildCollectionAttribute(
			ManagedTypeDescriptor runtimeContainer,
			ManagedTypeMapping bootContainer,
			RuntimeModelCreationContext context) {
		final PersistentCollectionRepresentation representation = context.getPersistentCollectionRepresentationResolver()
				.resolveRepresentation( (Collection) value );
		final PersistentCollectionDescriptor descriptor = representation.generatePersistentCollectionDescriptor(
				runtimeContainer,
				bootContainer,
				this,
				context
		);
		context.registerCollectionDescriptor( descriptor, (Collection) value );
		return descriptor.getDescribedAttribute();
	}

	@SuppressWarnings("unchecked")
	private <O,T> SingularPersistentAttribute<O,T> buildSingularAttribute(
			ManagedTypeDescriptor runtimeContainer,
			ManagedTypeMapping bootContainer,
			SingularPersistentAttribute.Disposition singularAttributeDisposition,
			RuntimeModelCreationContext context) {
		final PropertyAccess propertyAccess = runtimeContainer.getRepresentationStrategy().generatePropertyAccess(
				bootContainer,
				this,
				runtimeContainer,
				Environment.getBytecodeProvider()
		);
		if ( value instanceof BasicValueMapping ) {
			return new BasicSingularPersistentAttribute(
					runtimeContainer,
					this,
					propertyAccess,
					singularAttributeDisposition,
					context
			);
		}
		else if ( value instanceof ToOne ) {
			return new SingularPersistentAttributeEntity(
					runtimeContainer,
					this,
					propertyAccess,
					singularAttributeDisposition,
					isManyToOne( (ToOne) value )
							? SingularAttributeClassification.MANY_TO_ONE
							: SingularAttributeClassification.ONE_TO_ONE,
					context
			);
		}
		else if ( value instanceof Component ) {
			return new SingularPersistentAttributeEmbedded(
					runtimeContainer,
					this,
					propertyAccess,
					singularAttributeDisposition,
					context
			);
		}
		else if ( value instanceof Any ) {
			throw new NotYetImplementedFor6Exception();
		}

		throw new MappingException( "Unrecognized ValueMapping type for conversion to runtime model : " + value );
	}

	private boolean isManyToOne(ToOne value) {
		return ManyToOne.class.isInstance( value );
	}

}
