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
package org.hibernate.metamodel.internal.binder;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.PrimitiveTypeDescriptor;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.EmbeddedAttributeSource;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceBasic;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.spi.InFlightMetadataCollector;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.SingularAttributeNature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingBasic;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingManyToMany;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.AbstractValue;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import static org.hibernate.metamodel.spi.binding.EntityIdentifier.IdClassMetadata;

/**
 * Delegate for handling:<ol>
 * <li>
 * binding of Hibernate type information ({@link org.hibernate.metamodel.source.spi.HibernateTypeSource} ->
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
public class HibernateTypeHelper {
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
	public static class ReflectedCollectionJavaTypes {
		/**
		 * Singleton access
		 */
		public static final ReflectedCollectionJavaTypes NONE = new ReflectedCollectionJavaTypes();

		private final String collectionTypeName;
		private final String collectionElementTypeName;
		private final String collectionIndexTypeName;

		public ReflectedCollectionJavaTypes() {
			this( null, null, null );
		}

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

	JavaTypeDescriptor reflectedCollectionElementJavaType(
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		if ( reflectedCollectionJavaTypes != null &&
				reflectedCollectionJavaTypes.getCollectionElementTypeName() != null ) {
			return bindingContext().typeDescriptor(
					reflectedCollectionJavaTypes.getCollectionElementTypeName()
			);
		}
		else {
			return null;
		}
	}

	JavaTypeDescriptor reflectedCollectionIndexClassReference(
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		if ( reflectedCollectionJavaTypes != null && reflectedCollectionJavaTypes.getCollectionIndexTypeName() != null ) {
			return bindingContext().typeDescriptor( reflectedCollectionJavaTypes.getCollectionIndexTypeName() );
		}
		else {
			return null;
		}
	}

	JavaTypeDescriptor reflectedCollectionClassReference(
			final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		if( reflectedCollectionJavaTypes != null && reflectedCollectionJavaTypes.getCollectionTypeName() != null ) {
			return bindingContext().typeDescriptor( reflectedCollectionJavaTypes.getCollectionTypeName() );
		}
		else {
			return null;
		}
	}

	static ReflectedCollectionJavaTypes getReflectedCollectionJavaTypes(
			ClassLoaderService classLoaderService,
			final PluralAttributeBinding attributeBinding) {
		return determineReflectedCollectionJavaTypes( classLoaderService, attributeBinding.getAttribute() );
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
	 * @param explicitTypeParameters
	 *
	 * 		Explicit type parameters defined in the mapping or resolved from refrenced attribute type.
	 * 		<code>null</code> is accepted.
	 *
	 * @param defaultTypeDescriptor
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
			final Map<String, String> explicitTypeParameters,
			final JavaTypeDescriptor defaultTypeDescriptor,
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
					explicitTypeParameters,
					defaultTypeDescriptor
			);
			//2. resolve hibernate type
			type = getHeuristicType(
					determineTypeName( hibernateTypeDescriptor ),
					getTypeParameters( hibernateTypeDescriptor )
			);
		}
		if ( type == null ) {
			//todo how to deal with this?
		}

		//3. now set hibernateTypeDescriptor ResolvedTypeMapping and defaultJavaType (if not yet)
		hibernateTypeDescriptor.setResolvedTypeMapping( type );

		if ( hibernateTypeDescriptor.getJavaTypeDescriptor() == null ) {
			if ( defaultTypeDescriptor != null ) {
				hibernateTypeDescriptor.setJavaTypeDescriptor( defaultTypeDescriptor );
			}
			else if ( type != null && ! type.isAssociationType() ) {
				hibernateTypeDescriptor.setJavaTypeDescriptor(
						bindingContext().typeDescriptor(
								type.getReturnedClass()
										.getName()
						)
				);
			}
		}
	}

	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final String explicitTypeName,
			final Map<String, String> explictTypeParameters,
			final JavaTypeDescriptor defaultTypeDescriptor) {
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				explicitTypeName,
				explictTypeParameters,
				defaultTypeDescriptor,
				null
		);
	}


	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final HibernateTypeSource explicitTypeSource,
			final JavaTypeDescriptor defaultTypeDescriptor){
		bindHibernateTypeDescriptor( hibernateTypeDescriptor, explicitTypeSource, defaultTypeDescriptor, null);
	}

	void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final HibernateTypeSource explicitTypeSource,
			final JavaTypeDescriptor defaultTypeDescriptor,
			final Type resolvedType) {
		final String explicitTypeName = explicitTypeSource != null ? explicitTypeSource.getName() : null;
		final Map<String, String> parameters = explicitTypeSource != null ? explicitTypeSource.getParameters() : null;
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				explicitTypeName,
				parameters,
				defaultTypeDescriptor,
				resolvedType
		);
	}
	//--------------------------------------------------------------------------------

 	private final BinderRootContext helperContext;

	//package scope methods
	HibernateTypeHelper(BinderRootContext helperContext) {
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
							? EntityType.class.cast( resolvedHibernateType ).getIdentifierOrUniqueKeyType( metadataCollector() )
							: resolvedHibernateType;
			if ( AbstractValue.class.isInstance( value ) ) {
				( (AbstractValue) value ).setJdbcDataType(
						new JdbcDataType(
								resolvedRelationalType.sqlTypes( metadataCollector() )[0],
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
							? EntityType.class.cast( resolvedHibernateType ).getIdentifierOrUniqueKeyType( metadataCollector() )
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
			final ServiceRegistry serviceRegistry,
			final boolean isAttributeIdentifier,
			final Aggregate composite,
			final JavaTypeDescriptor defaultTypeDescriptor,
			final EmbeddedAttributeBinding attributeBinding) {
		Type resolvedType = typeFactory().component(
				new ComponentMetamodel(
						serviceRegistry,
						attributeBinding.getEmbeddableBinding(),
						isAttributeIdentifier,
						false
				)
		);
		bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				composite.getDescriptor().getName().toString(),
				null,
				defaultTypeDescriptor,
				resolvedType
		);
	}

	void bindBasicCollectionElementType(
			final PluralAttributeElementBindingBasic elementBinding,
			final PluralAttributeElementSourceBasic elementSource,
			final JavaTypeDescriptor defaultElementTypeDescriptor) {
		bindHibernateTypeDescriptor(
				elementBinding.getHibernateTypeDescriptor(),
				elementSource.getExplicitHibernateTypeSource(),
				defaultElementTypeDescriptor
		);
		bindJdbcDataType(
				elementBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				elementBinding.getRelationalValueBindings()
		);
	}


	public void bindNonAggregatedCompositeIdentifierType(
			ServiceRegistry serviceRegistry,
			EntityIdentifier.NonAggregatedCompositeIdentifierBinding idBinding) {
		final CompositeType idType;

		final CompositeType virtualIdType = (CompositeType) idBinding.getHibernateType( serviceRegistry, typeFactory() );
		final IdClassMetadata idClassMetadata = idBinding.getIdClassMetadata();
		if ( idClassMetadata != null ) {
			idType = (CompositeType) idClassMetadata.getHibernateType( serviceRegistry, typeFactory() );
		}
		else {
			idType = virtualIdType;
		}

		idBinding.getAttributeBinding().getHibernateTypeDescriptor().setResolvedTypeMapping( idType );
		bindHibernateTypeDescriptor(
				idBinding.getAttributeBinding().getHibernateTypeDescriptor(),
				idType.getReturnedClass().getName(),
				null,
				null,
				idType
		);

	}

	void bindNonAggregatedCompositeIdentifierType(
			final ServiceRegistry serviceRegistry,
			final EmbeddedAttributeBinding syntheticAttributeBinding,
			final SingularAttribute syntheticAttribute) {
		final Type resolvedType = typeFactory().embeddedComponent(
				new ComponentMetamodel( serviceRegistry, syntheticAttributeBinding.getEmbeddableBinding(), true, false )
		);
		final HibernateTypeDescriptor typeDescriptor = syntheticAttributeBinding.getHibernateTypeDescriptor();
		final String className = syntheticAttribute.getSingularAttributeType().getDescriptor() == null ?
				null :
				syntheticAttribute.getSingularAttributeType().getDescriptor().getName().toString();
		bindHibernateTypeDescriptor(
				typeDescriptor,
				className,
				null,
				null,
				resolvedType
		);
	}

	void bindManyToManyAttributeType(
			final PluralAttributeElementBindingManyToMany elementBinding,
			final PluralAttributeElementSourceManyToMany elementSource,
			final EntityBinding referencedEntityBinding,
			final JavaTypeDescriptor defaultElementTypeDescriptor) {
		final Type resolvedElementType = typeFactory().manyToOne(
				referencedEntityBinding.getEntityName(),
				elementSource.getReferencedEntityAttributeName(),
				false,
				false,
				elementSource.isIgnoreNotFound(),
				false
		);
		final HibernateTypeDescriptor hibernateTypeDescriptor = elementBinding.getHibernateTypeDescriptor();
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				referencedEntityBinding.getEntityName(),
				null,
				defaultElementTypeDescriptor,
				resolvedElementType
		);
		bindJdbcDataType(
				resolvedElementType,
				elementBinding.getRelationalValueContainer().relationalValueBindings()
		);
	}

	void bindDiscriminatorType(EntityDiscriminator discriminator, Value value) {
		bindHibernateTypeDescriptor(
				discriminator.getExplicitHibernateTypeDescriptor(),
				null,
				null,
				bindingContext().typeDescriptor( String.class.getName() )
		);
		bindJdbcDataType( discriminator.getExplicitHibernateTypeDescriptor().getResolvedTypeMapping(), value );
	}

	void bindSingularAttributeType(
			final SingularAttributeSource attributeSource,
			final SingularAttributeBinding attributeBinding) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding
				.getHibernateTypeDescriptor();
		final JavaTypeDescriptor attributeTypeDescriptor =  determineJavaType(
				attributeSource,
				attributeBinding.getAttribute().getAttributeContainer()
		);
		//try to resolve this attribute's java type first
		final JavaTypeDescriptor defaultTypeDescriptor;
		if ( attributeTypeDescriptor != null ) {
			attributeBinding.getAttribute().resolveType(
					bindingContext().locateOrBuildDomainType(
							attributeTypeDescriptor,
							attributeSource.getSingularAttributeNature() == SingularAttributeNature.COMPOSITE
					)
			);
			defaultTypeDescriptor = attributeTypeDescriptor;
		}
		else {
			defaultTypeDescriptor = null;
		}
		//do our best to fully fill hibernateTypeDescriptor
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				attributeSource.getTypeInformation(),
				defaultTypeDescriptor
		);

		processSingularAttributeTypeInformation(
				attributeSource,
				attributeBinding
		);
	}


	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ hibernate type resolver
	Type resolvePluralType(
			ClassLoaderService classLoaderService,
			final PluralAttributeBinding pluralAttributeBinding,
			final PluralAttributeSource pluralAttributeSource,
			final PluralAttributeNature nature) {
		if ( pluralAttributeBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( pluralAttributeBinding );
		}
		else {
			final String role = pluralAttributeBinding.getAttribute().getRole();
			final String propertyRef = getReferencedPropertyNameIfNotId( pluralAttributeBinding );
			switch ( nature ) {
				case BAG: {
					return typeFactory().bag( role, propertyRef );
				}
				case LIST: {
					return typeFactory().list( role, propertyRef );
				}
				case ARRAY: {
					// TODO: Move into a util?
					final JavaTypeDescriptor descriptor = pluralAttributeSource.getElementTypeDescriptor();
					final Class clazz;
					if (PrimitiveTypeDescriptor.class.isInstance( descriptor )) {
						clazz = ( (PrimitiveTypeDescriptor) descriptor ).getClassType();
					}
					else {
						clazz = classLoaderService.classForName( descriptor.getName().toString() );
					}
					return typeFactory().array(
							role,
							propertyRef,
							clazz
					);
				}
				case MAP: {
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
				}
				case SET: {
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
					else if ( pluralAttributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementNature.MANY_TO_MANY &&
							  ( (PluralAttributeElementBindingManyToMany) pluralAttributeBinding.getPluralAttributeElementBinding() ).getManyToManyOrderBy() != null ) {
						return typeFactory().orderedSet( role, propertyRef );
					}
					else {
						return typeFactory().set( role, propertyRef );
					}
				}
				default: {
					throw new NotYetImplementedException( nature + " is to be implemented" );
				}
			}
		}
	}

	private TypeFactory typeFactory(){
		return metadataCollector().getTypeResolver().getTypeFactory();
	}

	private Type determineHibernateTypeFromAttributeJavaType(
			final SingularAttribute singularAttribute) {
		if ( singularAttribute.getSingularAttributeType() != null ) {
			return getHeuristicType(
					singularAttribute.getSingularAttributeType().getDescriptor().getName().toString(),
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
	private Type getHeuristicType(
			final String typeName,
			final Properties typeParameters) {
		if ( typeName != null ) {
			try {
				return metadataCollector().getTypeResolver().heuristicType( typeName, typeParameters );
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
			final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
			resolvedType = getHeuristicType(
					determineTypeName( hibernateTypeDescriptor ),
					getTypeParameters( hibernateTypeDescriptor )
			);
			if ( resolvedType == null ) {
				resolvedType = determineHibernateTypeFromAttributeJavaType( attributeBinding.getAttribute() );
			}
		}

		if ( resolvedType != null ) {
			pushHibernateTypeInformationDown( attributeSource, attributeBinding, resolvedType );
		}

		//todo throw exception??
	}

	private void preProcessHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final String explicitTypeName,
			final Map<String, String> explictTypeParameters,
			final JavaTypeDescriptor defaultTypeDescriptor) {
		if ( explicitTypeName == null ) {
			if ( defaultTypeDescriptor != null ) {
				if ( hibernateTypeDescriptor.getJavaTypeDescriptor() != null ) {
					throw bindingContext().makeMappingException(
							String.format(
									"Attempt to re-initialize (non-explicit) Java type name; current=%s new=%s",
									hibernateTypeDescriptor.getJavaTypeDescriptor(),
									defaultTypeDescriptor
							)
					);
				}
				hibernateTypeDescriptor.setJavaTypeDescriptor( defaultTypeDescriptor );
			}
		}
		else {
			// Check if user-specified name is of a User-Defined Type (UDT)
			final TypeDefinition typeDef = metadataCollector().getTypeDefinition( explicitTypeName );
			if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Attempt to re-initialize explicitly-mapped Java type name; current=%s new=%s",
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
		if ( hibernateTypeDescriptor.getJavaTypeDescriptor() == null ) {
			// try to get Java type name from attribute because it may not be available from
			// resolvedHibernateType until after persisters are available.
			if ( attributeBinding.getAttribute().isTypeResolved() ) {
				hibernateTypeDescriptor.setJavaTypeDescriptor(
						attributeBinding.getAttribute().getSingularAttributeType().getDescriptor()
				);
			}
			else {
				hibernateTypeDescriptor.setJavaTypeDescriptor(
						bindingContext().typeDescriptor(
								resolvedHibernateType.getReturnedClass().getName()
						)
				);
			}
		}

		// sql type information ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( BasicAttributeBinding.class.isInstance( attributeBinding ) ) {
			pushHibernateTypeInformationDown(
					(BasicAttributeBinding) attributeBinding,
					resolvedHibernateType
			);
		}
		else if ( EmbeddedAttributeBinding.class.isInstance( attributeBinding ) ) {
			pushHibernateTypeInformationDown(
					(EmbeddedAttributeSource) attributeSource,
					(EmbeddedAttributeBinding) attributeBinding,
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
		if ( !singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeDescriptor() != null ) {
			singularAttribute.resolveType( makeDomainType( hibernateTypeDescriptor.getJavaTypeDescriptor().getName() ) );
		}
		bindJdbcDataType( resolvedHibernateType, attributeBinding.getRelationalValueBindings() );
	}

	@SuppressWarnings({ "UnusedParameters" })
	private void pushHibernateTypeInformationDown(
			final EmbeddedAttributeSource attributeSource,
			final EmbeddedAttributeBinding attributeBinding,
			final Type resolvedHibernateType) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();

		final SingularAttribute singularAttribute = attributeBinding.getAttribute();
		if ( !singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeDescriptor() != null ) {
			singularAttribute.resolveType( makeDomainType( hibernateTypeDescriptor.getJavaTypeDescriptor().getName() ) );
		}

		Iterator<AttributeSource> subAttributeSourceIterator = attributeSource.getEmbeddableSource().attributeSources().iterator();
		for ( AttributeBinding subAttributeBinding : attributeBinding.getEmbeddableBinding().attributeBindings() ) {
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

	private BinderLocalBindingContext bindingContext() {
		return helperContext.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext();
	}

	private InFlightMetadataCollector metadataCollector() {
		return bindingContext().getMetadataCollector();
	}

	private org.hibernate.metamodel.spi.domain.Type makeDomainType(String name) {
		return bindingContext().makeDomainType( name );
	}

	private org.hibernate.metamodel.spi.domain.Type makeDomainType(DotName name) {
		return bindingContext().makeDomainType( name );
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~ private static methods
	private static String determineTypeName(final HibernateTypeDescriptor hibernateTypeDescriptor) {
		if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
			return hibernateTypeDescriptor.getExplicitTypeName();
		}
		if ( hibernateTypeDescriptor.getJavaTypeDescriptor() != null ) {
			return hibernateTypeDescriptor.getJavaTypeDescriptor().getName().toString();
		}
		return null;
	}

	public String determineJavaTypeName(final IdentifiableTypeSource identifiableTypeSource) {
		if ( identifiableTypeSource.getTypeName() == null ) {
			if ( identifiableTypeSource.getHierarchy().getEntityMode() == EntityMode.MAP ) {
				return Map.class.getName();

			}
			else {
				return null;
			}
		}
		else {
			return identifiableTypeSource.getTypeName();
		}
	}

	public JavaTypeDescriptor determineJavaType(
			final EmbeddedAttributeSource attributeSource,
			final AttributeContainer attributeContainer,
			final EntityMode entityMode) {
		if ( attributeSource.getEmbeddableSource().getTypeDescriptor() != null ) {
			 return attributeSource.getEmbeddableSource().getTypeDescriptor();
		}
		else if ( entityMode == EntityMode.MAP ) {
			return bindingContext().typeDescriptor( Map.class.getName() );
		}
		else {
			return determineJavaType( attributeSource, attributeContainer );
		}
	}

	public JavaTypeDescriptor determineJavaType(
			final AttributeSource attributeSource,
			final AttributeContainer attributeContainer) {
		if ( attributeSource.getTypeInformation() != null && attributeSource.getTypeInformation().getJavaType() != null ) {
			return attributeSource.getTypeInformation().getJavaType();
		}
		else if ( attributeContainer.getDescriptor() == null ) {
			return null;
		}
		else {
			return determineAttributeType( attributeContainer.getDescriptor(), attributeSource.getName() );
		}
	}

	public static JavaTypeDescriptor determineAttributeType(
			JavaTypeDescriptor containerTypeDescriptor,
			String attributeName) {
		// todo : this needs to change once we figure out what to do with declared/non-declared attributes
		//		and generics...

		// check fields...
		for ( FieldDescriptor field : containerTypeDescriptor.getDeclaredFields() ) {
			if ( field.getName().equals( attributeName ) ) {
				return field.getType().getErasedType();
			}
		}

		// check methods...
		final String attributeNameCapitalized = capitalize( attributeName );
		final String setterName = "set" + attributeNameCapitalized;
		final String getterName = "get" + attributeNameCapitalized;
		final String isGetterName = "is" + attributeNameCapitalized;
		for ( MethodDescriptor method : containerTypeDescriptor.getDeclaredMethods() ) {
			// look for a setter...
			if ( method.getName().equals( setterName ) ) {
				if ( method.getArgumentTypes().size() == 1 ) {
					// technically the setters could be overloaded. but we'll assume this was it...
					return method.getArgumentTypes().iterator().next();
				}
			}

			// look for a getter
			if ( method.getName().equals( getterName ) || method.getName().equals( isGetterName ) ) {
				return method.getReturnType().getErasedType();
			}

		}

		if ( ClassDescriptor.class.isInstance( containerTypeDescriptor ) ) {
			final ClassDescriptor superType = ( (ClassDescriptor) containerTypeDescriptor ).getSuperType();
			if ( superType != null ) {
				return determineAttributeType( superType, attributeName );
			}
		}

		return null;
	}

	private static String capitalize(String attributeName) {
		return Character.toUpperCase( attributeName.charAt( 0 ) ) + attributeName.substring( 1 );
	}


	/**
	 * TODO : this should really use the field/methods descriptors to determine this information
	 * <p/>
	 * In the meantime we force load the Collection using ClassLoaderService and use Class to
	 * decide
	 */
	private static ReflectedCollectionJavaTypes determineReflectedCollectionJavaTypes(
			ClassLoaderService classLoaderService,
			PluralAttribute attribute) {
		if ( attribute.getAttributeContainer().getDescriptor() == null ) {
			return null; // EARLY RETURN
		}


		final String ownerClassName = attribute.getAttributeContainer().getDescriptor().getName().toString();
		try {
			final Class ownerClass = classLoaderService.classForName( ownerClassName );
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
					ownerClassName
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
		EntityIdentifier entityIdentifier = pluralAttributeBinding.getContainer()
				.seekEntityBinding()
				.getHierarchyDetails()
				.getEntityIdentifier();
		final String idAttributeName = entityIdentifier.getEntityIdentifierBinding()
				.getAttributeBinding()
				.getAttribute()
				.getName();
		return pluralAttributeBinding.getReferencedPropertyName().equals( idAttributeName ) ?
				null :
				pluralAttributeBinding.getReferencedPropertyName();
	}

	/**
	 * @see HibernateTypeHelper#determineReflectedCollectionJavaTypes
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
				Field field = ownerClass.getDeclaredField( attributeName );
				field.setAccessible( true );
				collectionAttributeType = field.getGenericType();
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