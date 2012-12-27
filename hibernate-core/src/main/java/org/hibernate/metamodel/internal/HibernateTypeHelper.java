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
package org.hibernate.metamodel.internal;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.AbstractValue;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

import org.jboss.logging.Logger;

/**
 * Delegate for handling:<ol>
 * <li>
 * binding of Hibernate type information ({@link ExplicitHibernateTypeSource} ->
 * {@link HibernateTypeDescriptor}
 * </li>
 * <li>
 * attempt to resolve the actual {@link Type} instance
 * </li>
 * <li>
 * push java type and JDBC type information reported by the {@link Type} instance to relational/
 * domain models.
 * </li>
 * </ol>
 * <p/>
 * Methods intended as entry points are:<ul>
 * <li>{@link #bindSingularAttributeTypeInformation}</li>
 * </ul>
 * <p/>
 * Currently the following methods are also required to be non-private because of handling discriminators which
 * are currently not modeled using attributes:<ul>
 * <li>{@link #bindJdbcDataType(org.hibernate.type.Type, org.hibernate.metamodel.spi.relational.Value)}</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Brett Meyer
 */
class HibernateTypeHelper {
	private static final Logger log = Logger.getLogger( HibernateTypeHelper.class );

	/* package-protected */
	static class ReflectedCollectionJavaTypes {
		private final String collectionTypeName;
		private final String collectionElementTypeName;
		private final String collectionIndexTypeName;

		private ReflectedCollectionJavaTypes(
				Class<?> collectionType,
				Class<?> collectionElementType,
				Class<?> collectionIndexType) {
			this.collectionTypeName = collectionType != null ? collectionType.getName() : null;
			this.collectionElementTypeName = collectionElementType != null ? collectionElementType.getName() : null;
			this.collectionIndexTypeName = collectionIndexType != null ? collectionIndexType.getName() : null;
		}

		String getCollectionElementTypeName() {
			return collectionElementTypeName;
		}

		String getCollectionIndexTypeName() {
			return collectionIndexTypeName;
		}

		String getCollectionTypeName() {
			return collectionTypeName;
		}
	}

	static String defaultCollectionElementJavaTypeName(
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		return reflectedCollectionJavaTypes != null ? reflectedCollectionJavaTypes.getCollectionElementTypeName() : null;
	}

	static String defaultCollectionIndexJavaTypeName(
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		return reflectedCollectionJavaTypes != null ? reflectedCollectionJavaTypes.getCollectionIndexTypeName() : null;
	}

	static String defaultCollectionJavaTypeName(
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes,
			final PluralAttributeSource attributeSource) {
		return reflectedCollectionJavaTypes != null ? reflectedCollectionJavaTypes.getCollectionTypeName() : attributeSource
				.getNature()
				.reportedJavaType()
				.getName();
	}

	static void bindHibernateResolvedType(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final Type resolvedType) {
		// Configure relational value JDBC type from Hibernate type descriptor now that its configured
		if ( resolvedType != null ) {
			hibernateTypeDescriptor.setResolvedTypeMapping( resolvedType );
			if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
				hibernateTypeDescriptor.setJavaTypeName( resolvedType.getReturnedClass().getName() );
			}
			hibernateTypeDescriptor.setToOne( resolvedType.isEntityType() );
		}
	}

	private final Binder binder;
	private final MetadataImplementor metadata;

	//package scope methods
	HibernateTypeHelper(Binder binder,
						MetadataImplementor metadata) {
		this.binder = binder;
		this.metadata = metadata;
	}

	/**
	 * Bind relational types using hibernate type just resolved.
	 *
	 * @param resolvedHibernateType The hibernate type resolved from metadata.
	 * @param value The relational value to be binded.
	 */
	void bindJdbcDataType(
			final Type resolvedHibernateType,
			final Value value) {
		if ( value.getJdbcDataType() == null && resolvedHibernateType != null && value != null ) {
			final Type resolvedRelationalType =
					resolvedHibernateType.isEntityType()
							? EntityType.class.cast( resolvedHibernateType ).getIdentifierOrUniqueKeyType( metadata )
							: resolvedHibernateType;
			if ( AbstractValue.class.isInstance( value ) ) {
				( (AbstractValue) value ).setJdbcDataType(
						new JdbcDataType(
								resolvedRelationalType.sqlTypes( metadata )[0],
								resolvedRelationalType.getName(),
								resolvedRelationalType.getReturnedClass()
						)
				);
			}
		}
	}

	void bindJdbcDataType(
			final Type resolvedHibernateType,
			final List<RelationalValueBinding> relationalValueBindings) {
		if ( relationalValueBindings.size() <= 1 ) {
			bindJdbcDataType( resolvedHibernateType, relationalValueBindings.get( 0 ).getValue() );
			return;
		}
		final Type resolvedRelationalType =
				resolvedHibernateType.isEntityType()
						? EntityType.class.cast( resolvedHibernateType ).getIdentifierOrUniqueKeyType( metadata )
						: resolvedHibernateType;
		if ( !CompositeType.class.isInstance( resolvedRelationalType ) ) {
			throw binder.bindingContext()
					.makeMappingException( "Column number mismatch" ); // todo refine the exception message
		}
		Type[] subTypes = CompositeType.class.cast( resolvedRelationalType ).getSubtypes();
		for ( int i = 0; i < subTypes.length; i++ ) {
			bindJdbcDataType( subTypes[i], relationalValueBindings.get( i ).getValue() );
		}
	}

	void bindBasicCollectionElementType(
			final BasicPluralAttributeElementBinding elementBinding,
			final BasicPluralAttributeElementSource elementSource,
			final String defaultElementJavaTypeName) {
		bindHibernateTypeDescriptor(
				elementBinding.getHibernateTypeDescriptor(),
				elementSource.getExplicitHibernateTypeSource(),
				defaultElementJavaTypeName
		);
		Type resolvedElementType = heuristicType( elementBinding.getHibernateTypeDescriptor() );
		bindHibernateResolvedType( elementBinding.getHibernateTypeDescriptor(), resolvedElementType );
		bindJdbcDataType(
				resolvedElementType,
				elementBinding.getRelationalValueBindings()
		);
	}

	Type resolvePluralType(
			final PluralAttributeBinding pluralAttributeBinding,
			final PluralAttributeSource pluralAttributeSource,
			final PluralAttributeSource.Nature nature) {
		if ( pluralAttributeBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( pluralAttributeBinding );
		}
		else {
			final TypeFactory typeFactory = metadata.getTypeResolver().getTypeFactory();
			final String role = pluralAttributeBinding.getAttribute().getRole();
			final String propertyRef = getReferencedPropertyNameIfNotId( pluralAttributeBinding );
			final boolean embedded = pluralAttributeBinding.getPluralAttributeElementBinding()
					.getNature() == PluralAttributeElementBinding.Nature.AGGREGATE;
			switch ( nature ) {
				case BAG:
					return typeFactory.bag( role, propertyRef, embedded );
				case LIST:
					return typeFactory.list( role, propertyRef, embedded );
				case ARRAY:
					return typeFactory.array(
							role,
							propertyRef,
							embedded,
							pluralAttributeSource.getElementClassReference().getValue()
					);
				case MAP:
					if ( pluralAttributeBinding.isSorted() ) {
						return typeFactory.sortedMap(
								role,
								propertyRef,
								embedded,
								pluralAttributeBinding.getComparator()
						);
					}
					// TODO: else if ( pluralAttributeBinding.hasOrder() ) { orderedMap... }
					else {
						return typeFactory.map( role, propertyRef, embedded );
					}
				case SET:
					if ( pluralAttributeBinding.isSorted() ) {
						return typeFactory.sortedSet(
								role,
								propertyRef,
								embedded,
								pluralAttributeBinding.getComparator()
						);
					}
					// TODO: else if ( pluralAttributeBinding.hasOrder() ) { orderedSet... }
					else {
						return typeFactory.set( role, propertyRef, embedded );
					}
				default:
					throw new NotYetImplementedException( nature + " is to be implemented" );
			}
		}
	}

	Type heuristicType(
			final HibernateTypeDescriptor hibernateTypeDescriptor) {
		final String typeName =
				hibernateTypeDescriptor.getExplicitTypeName() != null
						? hibernateTypeDescriptor.getExplicitTypeName()
						: hibernateTypeDescriptor.getJavaTypeName();
		final Properties properties = new Properties();
		properties.putAll( hibernateTypeDescriptor.getTypeParameters() );
		return metadata.getTypeResolver().heuristicType( typeName, properties );
	}
	// TODO: The following 3 methods should eventually be replaced w/
	// typeHelper use.

	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final ExplicitHibernateTypeSource explicitTypeSource,
			final ValueHolder<Class<?>> defaultJavaType) {
		// if there is an explicit type name specified, then there's no reason to
		// initialize the default Java type name; simply pass a null default instead.
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				explicitTypeSource,
				explicitTypeSource == null || explicitTypeSource.getName() == null
						? defaultJavaType.getValue().getName()
						: null
		);
	}

	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final ExplicitHibernateTypeSource explicitTypeSource,
			final String defaultJavaTypeName) {
		if ( explicitTypeSource == null ) {
			bindHibernateTypeDescriptor(
					hibernateTypeDescriptor, null, null, defaultJavaTypeName
			);
		}
		else {
			bindHibernateTypeDescriptor(
					hibernateTypeDescriptor,
					explicitTypeSource.getName(),
					explicitTypeSource.getParameters(),
					defaultJavaTypeName
			);
		}
	}

	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final String explicitTypeName,
			final Map<String, String> explictTypeParameters,
			final String defaultJavaTypeName) {
		if ( explicitTypeName == null ) {
			if ( hibernateTypeDescriptor.getJavaTypeName() != null ) {
				throw binder.bindingContext().makeMappingException(
						String.format(
								"Attempt to re-initialize (non-explicit) Java type name; current=%s new=%s",
								hibernateTypeDescriptor.getJavaTypeName(),
								defaultJavaTypeName
						)
				);
			}
			hibernateTypeDescriptor.setJavaTypeName( defaultJavaTypeName );
		}
		else {
			// Check if user-specified name is of a User-Defined Type (UDT)
			final TypeDefinition typeDef = metadata.getTypeDefinition( explicitTypeName );
			if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
				throw binder.bindingContext().makeMappingException(
						String.format(
								"Attempt to re-initialize explicity-mapped Java type name; current=%s new=%s",
								hibernateTypeDescriptor.getExplicitTypeName(),
								explicitTypeName
						)
				);
			}
			if ( typeDef == null ) {
				hibernateTypeDescriptor.setExplicitTypeName( explicitTypeName );
			}
			else {
				hibernateTypeDescriptor.setExplicitTypeName( typeDef.getTypeImplementorClass().getName() );
				// Don't use set() -- typeDef#parameters is unmodifiable
				hibernateTypeDescriptor.getTypeParameters().putAll( typeDef.getParameters() );
			}
			if ( explictTypeParameters != null ) {
				hibernateTypeDescriptor.getTypeParameters().putAll( explictTypeParameters );
			}
		}
	}

	void bindSingularAttributeTypeInformation(
			final SingularAttributeSource attributeSource,
			final SingularAttributeBinding attributeBinding) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding
				.getHibernateTypeDescriptor();

		final Class<?> attributeJavaType = attributeBinding.getContainer()
				.seekEntityBinding()
				.getHierarchyDetails()
				.getEntityMode() == EntityMode.POJO ? determineJavaType(
				attributeBinding.getAttribute()
		) : null;
		if ( attributeJavaType != null ) {
			attributeBinding.getAttribute().resolveType(
					makeJavaType(
							attributeJavaType.getName()
					)
			);
			if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
				hibernateTypeDescriptor.setJavaTypeName(
						attributeJavaType.getName()
				);
			}
		}

		bindHibernateTypeInformation(
				attributeSource.getTypeInformation(),
				hibernateTypeDescriptor
		);

		processSingularAttributeTypeInformation(
				attributeSource,
				attributeBinding
		);
	}

	ReflectedCollectionJavaTypes getReflectedCollectionJavaTypes(
			final PluralAttributeBinding attributeBinding) {
		return determineJavaType( attributeBinding.getAttribute() );
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ private scope methods
	private Class<?> determineJavaType(final SingularAttribute attribute) {
		try {
			final Class<?> ownerClass = attribute.getAttributeContainer().getClassReference();
			return ReflectHelper.reflectedPropertyClass( ownerClass, attribute.getName() );
		}
		catch ( Exception ignore ) {
			log.debugf(
					"Unable to locate attribute [%s] on class [%s]",
					attribute.getName(),
					attribute.getAttributeContainer().getClassName()
			);
		}
		return null;
	}

	private ReflectedCollectionJavaTypes determineJavaType(PluralAttribute attribute) {
		try {
			final Class<?> ownerClass = attribute.getAttributeContainer().getClassReference();
			PluralAttributeJavaTypeDeterminerDelegate delegate = new PluralAttributeJavaTypeDeterminerDelegate(
					ownerClass,
					attribute.getName()
			);
			BeanInfoHelper.visitBeanInfo( ownerClass, delegate );
			return delegate.collectionJavaTypes;
		}
		catch ( Exception ignore ) {
			log.debugf(
					"Unable to locate attribute [%s] on class [%s]",
					attribute.getName(),
					attribute.getAttributeContainer().getClassName()
			);
		}
		return null;
	}

	/**
	 * Takes explicit source type information and applies it to the binding model.
	 *
	 * @param typeSource The source (user supplied) hibernate type information
	 * @param hibernateTypeDescriptor The binding model hibernate type information
	 */
	private void bindHibernateTypeInformation(
			final ExplicitHibernateTypeSource typeSource,
			final HibernateTypeDescriptor hibernateTypeDescriptor) {

		final String explicitTypeName = typeSource.getName();

		if ( explicitTypeName != null ) {
			final TypeDefinition typeDefinition = metadata.getTypeDefinition(
					explicitTypeName
			);
			if ( typeDefinition != null ) {
				hibernateTypeDescriptor.setExplicitTypeName(
						typeDefinition.getTypeImplementorClass().getName()
				);
				// Don't use set() -- typeDef#parameters is unmodifiable
				hibernateTypeDescriptor.getTypeParameters().putAll(
						typeDefinition.getParameters()
				);
			}
			else {
				hibernateTypeDescriptor.setExplicitTypeName( explicitTypeName );
			}

			// TODO: Should type parameters be used for @TypeDefs?
			final Map<String, String> parameters = typeSource.getParameters();
			if ( parameters != null ) {
				// Don't use set() -- typeDef#parameters is unmodifiable
				hibernateTypeDescriptor.getTypeParameters().putAll(
						parameters
				);
			}
		}
	}

	/**
	 * Given an attribute, process all of its type information.  This includes resolving the actual
	 * {@link Type} instance and pushing JDBC/java information from that type down.
	 *
	 * @param attributeSource The attribute source.
	 * @param attributeBinding The attribute.
	 */
	private void processSingularAttributeTypeInformation(
			final SingularAttributeSource attributeSource,
			final SingularAttributeBinding attributeBinding) {
		Type resolvedType = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();

		if ( resolvedType == null ) {
			// we can determine the Hibernate Type if either:
			// 		1) the user explicitly named a Type in a HibernateTypeDescriptor
			// 		2) we know the java type of the attribute
			resolvedType = determineHibernateTypeFromDescriptor( attributeBinding.getHibernateTypeDescriptor() );
			if ( resolvedType == null ) {
				resolvedType = determineHibernateTypeFromAttributeJavaType( attributeBinding.getAttribute() );
			}
		}

		if ( resolvedType != null ) {
			pushHibernateTypeInformationDown( attributeSource, attributeBinding, resolvedType );
		}
	}

	private Type determineHibernateTypeFromDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor) {
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() != null ) {
			return hibernateTypeDescriptor.getResolvedTypeMapping();
		}
		String typeName = determineTypeName( hibernateTypeDescriptor );
		Properties typeParameters = getTypeParameters( hibernateTypeDescriptor );
		Type type = getHeuristicType( typeName, typeParameters );
		hibernateTypeDescriptor.setResolvedTypeMapping( type );
		return type;
	}

	private Type getHeuristicType(
			final String typeName,
			final Properties typeParameters) {
		if ( typeName != null ) {
			try {
				return metadata.getTypeResolver().heuristicType( typeName, typeParameters );
			}
			catch ( Exception ignore ) {
			}
		}

		return null;
	}

	private static final Properties EMPTY_PROPERTIES = new Properties();

	private Type determineHibernateTypeFromAttributeJavaType(
			final SingularAttribute singularAttribute) {
		if ( singularAttribute.getSingularAttributeType() != null ) {
			return getHeuristicType(
					singularAttribute.getSingularAttributeType().getClassName(),
					EMPTY_PROPERTIES
			);
		}
		return null;
	}

	private void pushHibernateTypeInformationDown(
			final SingularAttributeSource attributeSource,
			final SingularAttributeBinding attributeBinding,
			final Type resolvedHibernateType) {

		// sql type information ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( BasicAttributeBinding.class.isInstance( attributeBinding ) ) {
			pushHibernateTypeInformationDown(
					(BasicAttributeBinding) attributeBinding,
					resolvedHibernateType
			);
		}
		else if ( CompositeAttributeBinding.class.isInstance( attributeBinding ) ) {
			pushHibernateTypeInformationDown(
					(ComponentAttributeSource) attributeSource,
					(CompositeAttributeBinding) attributeBinding,
					resolvedHibernateType
			);
		}
	}

	private void pushHibernateTypeInformationDown(
			final BasicAttributeBinding attributeBinding,
			final Type resolvedHibernateType) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		final SingularAttribute singularAttribute = SingularAttribute.class.cast( attributeBinding.getAttribute() );
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() != null && hibernateTypeDescriptor.getJavaTypeName() == null ) {
			hibernateTypeDescriptor.setJavaTypeName( resolvedHibernateType.getReturnedClass().getName() );
		}
		if ( !singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeName() != null ) {
			singularAttribute.resolveType( makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		}
		pushHibernateTypeInformationDown(
				hibernateTypeDescriptor,
				attributeBinding.getRelationalValueBindings(),
				resolvedHibernateType
		);
	}

	@SuppressWarnings({ "UnusedParameters" })
	private void pushHibernateTypeInformationDown(
			final ComponentAttributeSource attributeSource,
			final CompositeAttributeBinding attributeBinding,
			final Type resolvedHibernateType) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		final SingularAttribute singularAttribute = SingularAttribute.class.cast( attributeBinding.getAttribute() );
		if ( !singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeName() != null ) {
			singularAttribute.resolveType( makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		}

		Iterator<AttributeSource> subAttributeSourceIterator = attributeSource.attributeSources().iterator();
		for ( AttributeBinding subAttributeBinding : attributeBinding.attributeBindings() ) {
			AttributeSource subAttributeSource = subAttributeSourceIterator.next();
			if ( SingularAttributeBinding.class.isInstance( subAttributeBinding ) ) {
				processSingularAttributeTypeInformation(
						(SingularAttributeSource) subAttributeSource,
						SingularAttributeBinding.class.cast( subAttributeBinding )
				);
			}
			else {
				throw new AssertionFailure(
						"Unknown type of AttributeBinding: " + attributeBinding.getClass()
								.getName()
				);
			}
		}
	}

	private void pushHibernateTypeInformationDown(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final List<RelationalValueBinding> relationalValueBindings,
			final Type resolvedHibernateType) {
		if ( resolvedHibernateType == null ) {
			return;
		}
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() == null ) {
			hibernateTypeDescriptor.setResolvedTypeMapping( resolvedHibernateType );
		}

		// java type information ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
			hibernateTypeDescriptor.setJavaTypeName( resolvedHibernateType.getReturnedClass().getName() );
		}

		hibernateTypeDescriptor.setToOne( resolvedHibernateType.isEntityType() );

		bindJdbcDataType( resolvedHibernateType, relationalValueBindings );
	}

	private org.hibernate.metamodel.spi.domain.Type makeJavaType(String name) {
		return binder.bindingContext().makeJavaType( name );
	}

	private Type resolveCustomCollectionType(
			final PluralAttributeBinding pluralAttributeBinding) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = pluralAttributeBinding.getHibernateTypeDescriptor();
		Properties typeParameters = new Properties();
		typeParameters.putAll( hibernateTypeDescriptor.getTypeParameters() );
		return metadata.getTypeResolver().getTypeFactory().customCollection(
				hibernateTypeDescriptor.getExplicitTypeName(),
				typeParameters,
				pluralAttributeBinding.getAttribute().getName(),
				getReferencedPropertyNameIfNotId( pluralAttributeBinding ),
				pluralAttributeBinding.getPluralAttributeElementBinding()
						.getNature() == PluralAttributeElementBinding.Nature.AGGREGATE
		);
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~ private static methods
	private static String determineTypeName(
			final HibernateTypeDescriptor hibernateTypeDescriptor) {
		return hibernateTypeDescriptor.getExplicitTypeName() != null
				? hibernateTypeDescriptor.getExplicitTypeName()
				: hibernateTypeDescriptor.getJavaTypeName();
	}

	private static Properties getTypeParameters(
			final HibernateTypeDescriptor hibernateTypeDescriptor) {
		if ( CollectionHelper.isEmpty( hibernateTypeDescriptor.getTypeParameters() ) ) {
			return EMPTY_PROPERTIES;
		}
		else {
			Properties typeParameters = new Properties();
			typeParameters.putAll( hibernateTypeDescriptor.getTypeParameters() );
			return typeParameters;
		}
	}

	private static String getReferencedPropertyNameIfNotId(
			final PluralAttributeBinding pluralAttributeBinding) {
		EntityIdentifier entityIdentifier =
				pluralAttributeBinding.getContainer().seekEntityBinding().getHierarchyDetails().getEntityIdentifier();
		final String idAttributeName =
				entityIdentifier.getAttributeBinding().getAttribute().getName();
		return pluralAttributeBinding.getReferencedPropertyName().equals( idAttributeName ) ?
				null :
				pluralAttributeBinding.getReferencedPropertyName();
	}

	/**
	 * @see HibernateTypeHelper#determineJavaType(PluralAttribute)
	 */
	private static class PluralAttributeJavaTypeDeterminerDelegate implements BeanInfoHelper.BeanInfoDelegate {
		private final Class<?> ownerClass;
		private final String attributeName;

		private ReflectedCollectionJavaTypes collectionJavaTypes;

		private PluralAttributeJavaTypeDeterminerDelegate(Class<?> ownerClass, String attributeName) {
			this.ownerClass = ownerClass;
			this.attributeName = attributeName;
		}

		@Override
		public void processBeanInfo(BeanInfo beanInfo) throws Exception {
			Class<?> collectionType = null;
			Class<?> elementJavaType = null;
			Class<?> indexJavaType = null;

			java.lang.reflect.Type collectionAttributeType = null;
			if ( beanInfo.getPropertyDescriptors() == null || beanInfo.getPropertyDescriptors().length == 0 ) {
				// we need to look for the field and look at it...
				collectionAttributeType = ownerClass.getField( attributeName ).getGenericType();
			}
			else {
				for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
					if ( propertyDescriptor.getName().equals( attributeName ) ) {
						if ( propertyDescriptor.getReadMethod() != null ) {
							collectionType = propertyDescriptor.getReadMethod().getReturnType();
							collectionAttributeType = propertyDescriptor.getReadMethod().getGenericReturnType();
						}
						else if ( propertyDescriptor.getWriteMethod() != null ) {
							collectionType = propertyDescriptor.getWriteMethod().getParameterTypes()[0];
							collectionAttributeType = propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0];
						}
					}
				}
			}

			if ( collectionAttributeType != null ) {
				if ( ParameterizedType.class.isInstance( collectionAttributeType ) ) {
					final java.lang.reflect.Type[] types = ( (ParameterizedType) collectionAttributeType ).getActualTypeArguments();
					if ( types == null ) {
					}
					else if ( types.length == 1 ) {
						elementJavaType = (Class<?>) types[0];
					}
					else if ( types.length == 2 ) {
						// Map<K,V>
						indexJavaType = (Class<?>) types[0];
						elementJavaType = (Class<?>) types[1];
					}
				}
				else if ( collectionType.isArray() ) {
					elementJavaType = collectionType.getComponentType();
				}
				else {
				}
			}
			collectionJavaTypes = new ReflectedCollectionJavaTypes( collectionType, elementJavaType, indexJavaType );
		}

	}

}