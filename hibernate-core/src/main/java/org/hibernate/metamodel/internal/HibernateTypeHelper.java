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

import org.jboss.logging.Logger;

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
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.ManyToManyPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.AbstractValue;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * Delegate for handling:<ol>
 * <li>
 * binding of Hibernate type information ({@link org.hibernate.metamodel.spi.source.HibernateTypeSource} ->
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
 * <li>{@link #bindSingularAttributeType}</li>
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

	/**
	 * package-protected
	 * <p/>
	 * Model a plural attribute's type info, including :
	 * <ul>
	 *     <li>collection type, like {@code list}, {@code set} etc</li>
	 *     <li>elements' type, which belongs in this collection</li>
	 *     <li>collection index type</li>
	 * </ul>
	 */
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
	static ReflectedCollectionJavaTypes getReflectedCollectionJavaTypes(
			final PluralAttributeBinding attributeBinding) {
		return determineJavaType( attributeBinding.getAttribute() );
	}
	//--------------------------------------------------------------------------------

	/**
	 * Bind type info into {@link HibernateTypeDescriptor}. The strategy below applied:
	 * <p/>
	 * <ul>
	 *     <li>if {@param resolvedType} is not null, then we can get <tt>resolvedTypeMapping</tt>, <tt>javaTypeName</tt>, <tt>toOne</tt> from it.</li>
	 *     <li>Or, we have to use provided {@param explicitTypeName} / {@param defaultJavaTypeName} to resolve hibernate type</li>
	 * </ul>
	 *
	 * @param hibernateTypeDescriptor
	 * 		The target {@link HibernateTypeDescriptor} to be bind. Can not be <code>null</code>.
	 *
	 * @param explicitTypeName
	 *
	 * 		Explicit type name defined in the mapping or resolved from referenced attribute.
	 * 		<code>null</code> is accepted.
	 *
	 * @param explictTypeParameters
	 *
	 * 		Explicit type parameters defined in the mapping or resolved from refrenced attribute type.
	 * 		<code>null</code> is accepted.
	 *
	 * @param defaultJavaTypeName
	 *
	 * 		Attribute java type. <code>null</code> is accepted.
	 *
	 * @param resolvedType
	 *
	 * 		Provided hibernate type. <code>null</code> is accepted.
	 */
	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final String explicitTypeName,
			final Map<String, String> explictTypeParameters,
			final String defaultJavaTypeName,
			final Type resolvedType) {
		Type type;
		if ( resolvedType != null ) {
			type = resolvedType;
		}
		else {
			//1. pre processing, resolve either explicitType or javaType
			preProcessHibernateTypeDescriptor(
					hibernateTypeDescriptor,
					explicitTypeName,
					explictTypeParameters,
					defaultJavaTypeName
			);
			//2. resolve hibernate type
			type = heuristicType( hibernateTypeDescriptor );
		}
		if ( type == null ) {
			//todo how to deal with this?
		}
		//3. now set hibernateTypeDescripter's ResolvedTypeMapping and defaultJavaType (if not yet)
		hibernateTypeDescriptor.setResolvedTypeMapping( type );
	}

	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final String explicitTypeName,
			final Map<String, String> explictTypeParameters,
			final String defaultJavaTypeName) {
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				explicitTypeName,
				explictTypeParameters,
				defaultJavaTypeName,
				null
		);
	}


	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final HibernateTypeSource explicitTypeSource,
			final String defaultJavaTypeName){
		bindHibernateTypeDescriptor( hibernateTypeDescriptor, explicitTypeSource, defaultJavaTypeName, null);
	}

	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final HibernateTypeSource explicitTypeSource,
			final String defaultJavaTypeName,
			final Type resolvedType) {
		final String explicitTypeName = explicitTypeSource != null ? explicitTypeSource.getName() : null;
		final Map<String, String> parameters = explicitTypeSource != null ? explicitTypeSource.getParameters() : null;
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				explicitTypeName,
				parameters,
				defaultJavaTypeName,
				resolvedType
		);
	}
	//--------------------------------------------------------------------------------

 	private final HelperContext helperContext;

	//package scope methods
	HibernateTypeHelper(HelperContext helperContext) {
		this.helperContext = helperContext;
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
		if ( value != null && value.getJdbcDataType() == null && resolvedHibernateType != null  ) {
			final Type resolvedRelationalType =
					resolvedHibernateType.isEntityType()
							? EntityType.class.cast( resolvedHibernateType ).getIdentifierOrUniqueKeyType( metadata() )
							: resolvedHibernateType;
			if ( AbstractValue.class.isInstance( value ) ) {
				( (AbstractValue) value ).setJdbcDataType(
						new JdbcDataType(
								resolvedRelationalType.sqlTypes( metadata() )[0],
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
		if ( resolvedHibernateType == null ) {
			throw new IllegalArgumentException( "resolvedHibernateType must be non-null." );
		}
		if ( relationalValueBindings == null || relationalValueBindings.isEmpty() ) {
			throw new IllegalArgumentException( "relationalValueBindings must be non-null." );
		}
		if ( relationalValueBindings.size() == 1 ) {
			bindJdbcDataType( resolvedHibernateType, relationalValueBindings.get( 0 ).getValue() );
		}
		else {
			final Type resolvedRelationalType =
					resolvedHibernateType.isEntityType()
							? EntityType.class.cast( resolvedHibernateType ).getIdentifierOrUniqueKeyType( metadata() )
							: resolvedHibernateType;
			if ( !CompositeType.class.isInstance( resolvedRelationalType ) ) {
				throw bindingContext()
						.makeMappingException( "Column number mismatch" ); // todo refine the exception message
			}
			Type[] subTypes = CompositeType.class.cast( resolvedRelationalType ).getSubtypes();
			for ( int i = 0; i < subTypes.length; i++ ) {
				bindJdbcDataType( subTypes[i], relationalValueBindings.get( i ).getValue() );
			}
		}
	}

	void bindAggregatedCompositeAttributeType(
			final boolean isAttributeIdentifier,
			final Aggregate composite,
			final ValueHolder<Class<?>> defaultJavaClassReference,
			final CompositeAttributeBinding attributeBinding) {
		Type resolvedType = typeFactory().component(
				new ComponentMetamodel( attributeBinding, isAttributeIdentifier, false )
		);
		bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				composite.getClassName(),
				null,
				defaultJavaClassReference == null ? null : defaultJavaClassReference.getValue().getName(),
				resolvedType
		);
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
		bindJdbcDataType(
				elementBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				elementBinding.getRelationalValueBindings()
		);
	}
	void bindNonAggregatedCompositeIdentifierType(
			final CompositeAttributeBinding syntheticAttributeBinding,
			final SingularAttribute syntheticAttribute) {
		final Type resolvedType = typeFactory().embeddedComponent(
				new ComponentMetamodel( syntheticAttributeBinding, true, false )
		);
		final HibernateTypeDescriptor typeDescriptor = syntheticAttributeBinding.getHibernateTypeDescriptor();
		bindHibernateTypeDescriptor(
				typeDescriptor,
				syntheticAttribute.getSingularAttributeType().getClassName(),
				null,
				null,
				resolvedType
		);
	}
	void bindManyToManyAttributeType(
			final ManyToManyPluralAttributeElementBinding elementBinding,
			final ManyToManyPluralAttributeElementSource elementSource,
			final EntityBinding referencedEntityBinding,
			final String defaultElementJavaTypeName) {
		final Type resolvedElementType = typeFactory().manyToOne(
				referencedEntityBinding.getEntity().getName(),
				elementSource.getReferencedEntityAttributeName(),
				false,
				false,
				!elementSource.isNotFoundAnException(),
				false
		);
		final HibernateTypeDescriptor hibernateTypeDescriptor = elementBinding.getHibernateTypeDescriptor();
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				referencedEntityBinding.getEntity().getName(),
				null,
				defaultElementJavaTypeName,
				resolvedElementType
		);
		bindJdbcDataType(
				resolvedElementType,
				elementBinding.getRelationalValueBindings()
		);
	}

	void bindDiscriminatorType(EntityDiscriminator discriminator, Value value) {
		bindHibernateTypeDescriptor(
				discriminator.getExplicitHibernateTypeDescriptor(),
				null,
				null,
				String.class.getName()
		);
		bindJdbcDataType( discriminator.getExplicitHibernateTypeDescriptor().getResolvedTypeMapping(), value );
	}

	void bindSingularAttributeType(
			final SingularAttributeSource attributeSource,
			final SingularAttributeBinding attributeBinding) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding
				.getHibernateTypeDescriptor();
		final Class<?> attributeJavaType =  determineJavaType( attributeSource, attributeBinding);
		//try to resolve this attribute's java type first
		final String defaultJavaTypeName;
		if ( attributeJavaType != null ) {
			attributeBinding.getAttribute().resolveType(
					makeJavaType(
							attributeJavaType.getName()
					)
			);
			defaultJavaTypeName = attributeJavaType.getName();
		} else {
			defaultJavaTypeName = null;
		}
		//do our best to full fill hibernateTypeDescriptor
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				attributeSource.getTypeInformation(),
				defaultJavaTypeName
		);

		processSingularAttributeTypeInformation(
				attributeSource,
				attributeBinding
		);
	}


	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ hibernate type resolver
	Type resolvePluralType(
			final PluralAttributeBinding pluralAttributeBinding,
			final PluralAttributeSource pluralAttributeSource,
			final PluralAttributeSource.Nature nature) {
		if ( pluralAttributeBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( pluralAttributeBinding );
		}
		else {
			final String role = pluralAttributeBinding.getAttribute().getRole();
			final String propertyRef = getReferencedPropertyNameIfNotId( pluralAttributeBinding );
			switch ( nature ) {
				case BAG:
					return typeFactory().bag( role, propertyRef );
				case LIST:
					return typeFactory().list( role, propertyRef );
				case ARRAY:
					return typeFactory().array(
							role,
							propertyRef,
							pluralAttributeSource.getElementClassReference().getValue()
					);
				case MAP:
					if ( pluralAttributeBinding.isSorted() ) {
						return typeFactory().sortedMap(
								role,
								propertyRef,
								pluralAttributeBinding.getComparator()
						);
					}
					// TODO: else if ( pluralAttributeBinding.hasOrder() ) { orderedMap... }
					else {
						return typeFactory().map( role, propertyRef );
					}
				case SET:
					if ( pluralAttributeBinding.isSorted() ) {
						return typeFactory().sortedSet(
								role,
								propertyRef,
								pluralAttributeBinding.getComparator()
						);
					}
					else if ( pluralAttributeBinding.getOrderBy() != null ) {
						return typeFactory().orderedSet( role, propertyRef );
					}
					else if ( pluralAttributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementBinding.Nature.MANY_TO_MANY &&
							  ( (ManyToManyPluralAttributeElementBinding) pluralAttributeBinding.getPluralAttributeElementBinding() ).getManyToManyOrderBy() != null ) {
						return typeFactory().orderedSet( role, propertyRef );
					}
					else {
						return typeFactory().set( role, propertyRef );
					}
				default:
					throw new NotYetImplementedException( nature + " is to be implemented" );
			}
		}
	}

	private TypeFactory typeFactory(){
		return metadata().getTypeResolver().getTypeFactory();
	}

	private Type determineHibernateTypeFromAttributeJavaType(
			final SingularAttribute singularAttribute) {
		if ( singularAttribute.getSingularAttributeType() != null ) {
			return getHeuristicType(
					singularAttribute.getSingularAttributeType().getClassName(),
					null
			);
		}
		return null;
	}
	private Type resolveCustomCollectionType(
			final PluralAttributeBinding pluralAttributeBinding) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = pluralAttributeBinding.getHibernateTypeDescriptor();
		Properties typeParameters = getTypeParameters( hibernateTypeDescriptor );
		return typeFactory().customCollection(
				hibernateTypeDescriptor.getExplicitTypeName(),
				typeParameters,
				pluralAttributeBinding.getAttribute().getName(),
				getReferencedPropertyNameIfNotId( pluralAttributeBinding )
		);
	}
	/**
	 * Resolve hibernate type with info from {@link HibernateTypeDescriptor} using {@link org.hibernate.type.TypeResolver}.
	 * <p/>
	 * return <code>null</code> if can't resolve.
	 */
	private Type heuristicType(
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
				return metadata().getTypeResolver().heuristicType( typeName, typeParameters );
			}
			catch ( Exception ignore ) {
			}
		}

		return null;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ private scope methods

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
			resolvedType = heuristicType( attributeBinding.getHibernateTypeDescriptor() );
			if ( resolvedType == null ) {
				resolvedType = determineHibernateTypeFromAttributeJavaType( attributeBinding.getAttribute() );
			}
		}

		if ( resolvedType != null ) {
			pushHibernateTypeInformationDown( attributeSource, attributeBinding, resolvedType );
		} else{
			//todo throw exception??
		}
	}

	private void preProcessHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final String explicitTypeName,
			final Map<String, String> explictTypeParameters,
			final String defaultJavaTypeName) {
		if ( explicitTypeName == null ) {
			if ( defaultJavaTypeName != null && hibernateTypeDescriptor.getJavaTypeName() != null ) {
				throw bindingContext().makeMappingException(
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
			final TypeDefinition typeDef = metadata().getTypeDefinition( explicitTypeName );
			if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
				throw bindingContext().makeMappingException(
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

	private void pushHibernateTypeInformationDown(
			final SingularAttributeSource attributeSource,
			final SingularAttributeBinding attributeBinding,
			final Type resolvedHibernateType) {
		if ( resolvedHibernateType == null ) {
			throw bindingContext().makeMappingException( "Resolved hibernate type can't be null" );
		}
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() == null ) {
			hibernateTypeDescriptor.setResolvedTypeMapping( resolvedHibernateType );
		}

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

	/**
	 * Resolve domain type for this attribute and also bind jdbc type.
	 *
	 * hibernateTypeDescriptor from this binding must already be set with same resolvedHibernateType.
	 */
	private void pushHibernateTypeInformationDown(
			final BasicAttributeBinding attributeBinding,
			final Type resolvedHibernateType) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		final SingularAttribute singularAttribute = attributeBinding.getAttribute();
		if ( !singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeName() != null ) {
			singularAttribute.resolveType( makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		}
		bindJdbcDataType( resolvedHibernateType, attributeBinding.getRelationalValueBindings() );
	}

	@SuppressWarnings({ "UnusedParameters" })
	private void pushHibernateTypeInformationDown(
			final ComponentAttributeSource attributeSource,
			final CompositeAttributeBinding attributeBinding,
			final Type resolvedHibernateType) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();

		final SingularAttribute singularAttribute = attributeBinding.getAttribute();
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

	private LocalBindingContext bindingContext() {
		return helperContext.bindingContext();
	}

	private MetadataImplementor metadata() {
		return bindingContext().getMetadataImplementor();
	}

	private org.hibernate.metamodel.spi.domain.Type makeJavaType(String name) {
		return bindingContext().makeJavaType( name );
	}



	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~ private static methods
	private static String determineTypeName(
			final HibernateTypeDescriptor hibernateTypeDescriptor) {
		return hibernateTypeDescriptor.getExplicitTypeName() != null
				? hibernateTypeDescriptor.getExplicitTypeName()
				: hibernateTypeDescriptor.getJavaTypeName();
	}


	private static Class<?> determineJavaType(final SingularAttributeSource attributeSource,
											  final SingularAttributeBinding attributeBinding) {
		Class clazz = attributeSource.getTypeInformation().getJavaType();
		if ( clazz != null ) {
			return clazz;
		}
		final SingularAttribute attribute = attributeBinding.getAttribute();
		try {
			final EntityMode entityMode = attributeBinding.getContainer()
					.seekEntityBinding()
					.getHierarchyDetails()
					.getEntityMode();
			if ( entityMode != EntityMode.POJO ) {
				return null;
			}
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


	private static ReflectedCollectionJavaTypes determineJavaType(PluralAttribute attribute) {
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
	 *
	 * @return  type parameters defined in the hibernate type descriptor or {@code  empty property}.
	 */
	private static Properties getTypeParameters(
			final HibernateTypeDescriptor hibernateTypeDescriptor) {
		if ( CollectionHelper.isEmpty( hibernateTypeDescriptor.getTypeParameters() ) ) {
			return null;
		}
		else {
			Properties typeParameters = new Properties();
			typeParameters.putAll( hibernateTypeDescriptor.getTypeParameters() );
			return typeParameters;
		}
	}

	/**
	 * Find the referenced attribute name, if it is not id attribute.
	 * @param pluralAttributeBinding Plural attribute binding that has this reference info
	 * @return Plural attribute referenced attribute name, or <code>null</code> if it is id.
	 */
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
				//TODO this only works when the field is public accessable or NoSuchElementException will be thrown
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
				else if ( collectionType != null && collectionType.isArray() ) {
					elementJavaType = collectionType.getComponentType();
				}
				else {
				}
			}
			collectionJavaTypes = new ReflectedCollectionJavaTypes( collectionType, elementJavaType, indexJavaType );
		}

	}

}