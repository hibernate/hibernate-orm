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
package org.hibernate.metamodel.internal.source;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.AbstractValue;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * Delegate for handling:<ol>
 *     <li>
 *         binding of Hibernate type information ({@link ExplicitHibernateTypeSource} ->
 *         {@link HibernateTypeDescriptor}
 *     </li>
 *     <li>
 *         attempt to resolve the actual {@link Type} instance
 *     </li>
 *     <li>
 *         push java type and JDBC type information reported by the {@link Type} instance to relational/
 *         domain models.
 *     </li>
 * </ol>
 * <p/>
 * Methods intended as entry points are:<ul>
 *     <li>{@link #bindSingularAttributeTypeInformation}</li>
 *     <li>{@link #bindPluralAttributeTypeInformation}</li>
 * </ul>
 * <p/>
 * Currently the following methods are also required to be non-private because of handling discriminators which
 * are currently not modeled using attributes:<ul>
 *     <li>{@link #determineHibernateTypeFromDescriptor}</li>
 *     <li>{@link #pushHibernateTypeInformationDown(org.hibernate.type.Type, org.hibernate.metamodel.spi.relational.Value)}</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class HibernateTypeHelper {
	private static final Logger log = Logger.getLogger( HibernateTypeHelper.class );

	private final Binder binder;
	private final MetadataImplementor metadata;

	public HibernateTypeHelper( Binder binder,
	                            MetadataImplementor metadata ) {
		this.binder = binder;
		this.metadata = metadata;
	}

	private org.hibernate.metamodel.spi.domain.Type makeJavaType(String name) {
		return binder.bindingContext().makeJavaType( name );
	}

	public void bindSingularAttributeTypeInformation(
			SingularAttributeSource attributeSource,
			SingularAttributeBinding attributeBinding) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();

		final Class<?> attributeJavaType = determineJavaType( attributeBinding.getAttribute() );
		if ( attributeJavaType != null ) {
			attributeBinding.getAttribute().resolveType( makeJavaType( attributeJavaType.getName() ) );
			if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
				hibernateTypeDescriptor.setJavaTypeName( attributeJavaType.getName() );
			}
		}

		bindHibernateTypeInformation( attributeSource.getTypeInformation(), hibernateTypeDescriptor );

		processSingularAttributeTypeInformation( attributeSource, attributeBinding );
	}

	public void bindPluralAttributeTypeInformation(
			PluralAttributeSource attributeSource,
			PluralAttributeBinding attributeBinding) {
		final ReflectedCollectionJavaTypes reflectedCollectionJavaTypes = determineJavaType( attributeBinding.getAttribute() );

		if ( reflectedCollectionJavaTypes != null ) {
			if ( reflectedCollectionJavaTypes.collectionType != null ) {
				if ( attributeBinding.getHibernateTypeDescriptor().getJavaTypeName() == null ) {
					attributeBinding.getHibernateTypeDescriptor().setJavaTypeName(
							reflectedCollectionJavaTypes.collectionType.getName()
					);
				}
			}
			if ( reflectedCollectionJavaTypes.collectionElementType != null ) {
				if ( attributeBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeName() == null ) {
					attributeBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().setJavaTypeName(
							reflectedCollectionJavaTypes.collectionElementType.getName()
					);
				}
			}
			if ( reflectedCollectionJavaTypes.collectionIndexType != null
					&& IndexedPluralAttributeBinding.class.isInstance( attributeBinding ) ) {
				final PluralAttributeIndexBinding indexBinding
						= ( (IndexedPluralAttributeBinding) attributeBinding ).getPluralAttributeIndexBinding();
				if ( indexBinding.getHibernateTypeDescriptor().getJavaTypeName() == null ) {
					indexBinding.getHibernateTypeDescriptor().setJavaTypeName(
							reflectedCollectionJavaTypes.collectionIndexType.getName()
					);
				}
			}
		}

		bindHibernateTypeInformation(
				attributeSource.getTypeInformation(),
				attributeBinding.getHibernateTypeDescriptor()
		);
		processPluralAttributeTypeInformation( attributeSource, attributeBinding );
	}

	private void processPluralAttributeKeyTypeInformation(PluralAttributeKeyBinding keyBinding) {
		final HibernateTypeDescriptor pluralAttributeKeyTypeDescriptor = keyBinding.getHibernateTypeDescriptor();
		final HibernateTypeDescriptor referencedTypeDescriptor =
				keyBinding.getReferencedAttributeBinding().getHibernateTypeDescriptor();

		pluralAttributeKeyTypeDescriptor.setExplicitTypeName( referencedTypeDescriptor.getExplicitTypeName() );
		pluralAttributeKeyTypeDescriptor.setJavaTypeName( referencedTypeDescriptor.getJavaTypeName() );

		// TODO: not sure about the following...
		pluralAttributeKeyTypeDescriptor.setToOne( referencedTypeDescriptor.isToOne() );
		pluralAttributeKeyTypeDescriptor.getTypeParameters().putAll( referencedTypeDescriptor.getTypeParameters() );

		processPluralAttributeKeyInformation( keyBinding );
	}

	private void processPluralAttributeKeyInformation(PluralAttributeKeyBinding keyBinding) {
		if ( keyBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() != null ) {
			return;
		}
		// we can determine the Hibernate Type if either:
		// 		1) the user explicitly named a Type in a HibernateTypeDescriptor
		// 		2) we know the java type of the attribute
		Type resolvedType = determineHibernateTypeFromDescriptor( keyBinding.getHibernateTypeDescriptor() );
		if ( resolvedType == null ) {
			resolvedType = determineHibernateTypeFromAttributeJavaType(
					keyBinding.getReferencedAttributeBinding().getAttribute()
			);
		}

		if ( resolvedType != null ) {
			Iterator<Column> fkColumnIterator = keyBinding.getForeignKey().getSourceColumns().iterator();

			if ( resolvedType.isComponentType() ) {
				ComponentType componentType = ( ComponentType ) resolvedType;
				for ( Type subType : componentType.getSubtypes() ) {
					pushHibernateTypeInformationDown( subType, fkColumnIterator.next() );
				}
			}
			else {
				pushHibernateTypeInformationDown( resolvedType, fkColumnIterator.next() );
			}
		}
	}

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
			ExplicitHibernateTypeSource typeSource,
			HibernateTypeDescriptor hibernateTypeDescriptor) {
		final String explicitTypeName = typeSource.getName();
		if ( explicitTypeName != null ) {
			final TypeDefinition typeDefinition = metadata.getTypeDefinition( explicitTypeName );
			if ( typeDefinition != null ) {
				hibernateTypeDescriptor.setExplicitTypeName( typeDefinition.getTypeImplementorClass().getName() );
				hibernateTypeDescriptor.getTypeParameters().putAll( typeDefinition.getParameters() );
			}
			else {
				hibernateTypeDescriptor.setExplicitTypeName( explicitTypeName );
			}
			final Map<String, String> parameters = typeSource.getParameters();
			if ( parameters != null ) {
				hibernateTypeDescriptor.getTypeParameters().putAll( parameters );
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
			SingularAttributeSource attributeSource,
			SingularAttributeBinding attributeBinding) {
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

	public Type determineHibernateTypeFromDescriptor(HibernateTypeDescriptor hibernateTypeDescriptor) {
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() != null ) {
			return hibernateTypeDescriptor.getResolvedTypeMapping();
		}
		String typeName = determineTypeName( hibernateTypeDescriptor );
		Properties typeParameters = getTypeParameters( hibernateTypeDescriptor );
		Type type = getHeuristicType( typeName, typeParameters );
		hibernateTypeDescriptor.setResolvedTypeMapping( type );
		return type;
	}

	private Type getHeuristicType(String typeName, Properties typeParameters) {
		if ( typeName != null ) {
			try {
				return metadata.getTypeResolver().heuristicType( typeName, typeParameters );
			}
			catch (Exception ignore) {
			}
		}

		return null;
	}

	private final Properties EMPTY_PROPERTIES = new Properties();

	private Type determineHibernateTypeFromAttributeJavaType(SingularAttribute singularAttribute) {
		if ( singularAttribute.getSingularAttributeType() != null ) {
			return getHeuristicType(
					singularAttribute.getSingularAttributeType().getClassName(),
					EMPTY_PROPERTIES
			);
		}
		return null;
	}

	private static String determineTypeName(HibernateTypeDescriptor hibernateTypeDescriptor) {
		return hibernateTypeDescriptor.getExplicitTypeName() != null
				? hibernateTypeDescriptor.getExplicitTypeName()
				: hibernateTypeDescriptor.getJavaTypeName();
	}

	private static Properties getTypeParameters(HibernateTypeDescriptor hibernateTypeDescriptor) {
		Properties typeParameters = new Properties( );
		if ( hibernateTypeDescriptor.getTypeParameters() != null ) {
			typeParameters.putAll( hibernateTypeDescriptor.getTypeParameters() );
		}
		return typeParameters;
	}

	private void pushHibernateTypeInformationDown(
			SingularAttributeSource attributeSource,
			SingularAttributeBinding attributeBinding,
			Type resolvedHibernateType) {

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
			BasicAttributeBinding attributeBinding,
			Type resolvedHibernateType) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		final SingularAttribute singularAttribute = SingularAttribute.class.cast( attributeBinding.getAttribute() );
		if ( ! singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeName() != null ) {
			singularAttribute.resolveType( makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		}
		pushHibernateTypeInformationDown(
				hibernateTypeDescriptor,
				attributeBinding.getRelationalValueBindings(),
				resolvedHibernateType
		);
	}

	@SuppressWarnings( {"UnusedParameters"})
	private void pushHibernateTypeInformationDown(
			ComponentAttributeSource attributeSource,
			CompositeAttributeBinding attributeBinding,
			Type resolvedHibernateType) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		final SingularAttribute singularAttribute = SingularAttribute.class.cast( attributeBinding.getAttribute() );
		if ( ! singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeName() != null ) {
			singularAttribute.resolveType( makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		}

		Iterator<AttributeSource> subAttributeSourceIterator = attributeSource.attributeSources().iterator();
		for ( AttributeBinding subAttributeBinding : attributeBinding.attributeBindings() ) {
			AttributeSource subAttributeSource = subAttributeSourceIterator.next();
			if ( SingularAttributeBinding.class.isInstance( subAttributeBinding ) ) {
				processSingularAttributeTypeInformation(
						( SingularAttributeSource ) subAttributeSource,
						SingularAttributeBinding.class.cast( subAttributeBinding )
				);
			}
			else if ( AbstractPluralAttributeBinding.class.isInstance( subAttributeBinding ) ) {
				processPluralAttributeTypeInformation(
						( PluralAttributeSource ) subAttributeSource,
						AbstractPluralAttributeBinding.class.cast( subAttributeBinding )
				);
			}
			else {
				throw new AssertionFailure( "Unknown type of AttributeBinding: " + attributeBinding.getClass().getName() );
			}
		}
	}

	private void pushHibernateTypeInformationDown(
			HibernateTypeDescriptor hibernateTypeDescriptor,
			List<RelationalValueBinding> relationalValueBindings,
			Type resolvedHibernateType) {
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

		// todo : this can be made a lot smarter, but for now this will suffice.  currently we only handle single value bindings

		if ( relationalValueBindings.size() > 1 ) {
			return;
		}
		final Value value = relationalValueBindings.get( 0 ).getValue();
		pushHibernateTypeInformationDown( resolvedHibernateType, value );
	}

	public void pushHibernateTypeInformationDown(Type resolvedHibernateType, Value value) {
		if ( value.getJdbcDataType() == null ) {
			if ( AbstractValue.class.isInstance( value ) ) {
				( (AbstractValue) value ).setJdbcDataType(
						new JdbcDataType(
								resolvedHibernateType.sqlTypes( metadata )[0],
								resolvedHibernateType.getName(),
								resolvedHibernateType.getReturnedClass()
						)
				);
			}
		}
	}

	private void processPluralAttributeTypeInformation(
			PluralAttributeSource attributeSource,
			PluralAttributeBinding attributeBinding) {
		processCollectionTypeInformation( attributeBinding );
		processPluralAttributeElementTypeInformation( attributeSource.getElementSource(), attributeBinding.getPluralAttributeElementBinding() );
		processPluralAttributeKeyTypeInformation( attributeBinding.getPluralAttributeKeyBinding() );
	}

	private void processCollectionTypeInformation(PluralAttributeBinding attributeBinding) {
		if ( attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() != null ) {
			return;
		}

		Type resolvedType;
		// do NOT look at java type...
		//String typeName = determineTypeName( attributeBinding.getHibernateTypeDescriptor() );
		String typeName = attributeBinding.getHibernateTypeDescriptor().getExplicitTypeName();
		if ( typeName != null ) {
			resolvedType =
					metadata.getTypeResolver()
							.getTypeFactory()
							.customCollection(
									typeName,
									getTypeParameters( attributeBinding.getHibernateTypeDescriptor() ),
									attributeBinding.getAttribute().getName(),
									attributeBinding.getReferencedPropertyName(),
									attributeBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() ==
											PluralAttributeElementNature.COMPOSITE
							);
		}
		else {
			resolvedType = determineHibernateTypeFromCollectionType( attributeBinding );
		}
		if ( resolvedType != null ) {
			attributeBinding.getHibernateTypeDescriptor().setResolvedTypeMapping( resolvedType );
		}
	}

	private Type determineHibernateTypeFromCollectionType(PluralAttributeBinding attributeBinding) {
		final TypeFactory typeFactory = metadata.getTypeResolver().getTypeFactory();
		switch ( attributeBinding.getAttribute().getNature() ) {
			case SET: {
				return typeFactory.set(
						attributeBinding.getAttribute().getRole(),
						attributeBinding.getReferencedPropertyName(),
						attributeBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() == PluralAttributeElementNature.COMPOSITE
				);
			}
			case BAG: {
				return typeFactory.bag(
						attributeBinding.getAttribute().getRole(),
						attributeBinding.getReferencedPropertyName(),
						attributeBinding.getPluralAttributeElementBinding()
								.getPluralAttributeElementNature() == PluralAttributeElementNature.COMPOSITE
				);
			}
			default: {
				throw new UnsupportedOperationException(
						"Collection type not supported yet:" + attributeBinding.getAttribute().getNature()
				);
			}
		}
	}

	private void processPluralAttributeElementTypeInformation(
			PluralAttributeElementSource elementSource,
			PluralAttributeElementBinding pluralAttributeElementBinding
	) {
		switch ( pluralAttributeElementBinding.getPluralAttributeElementNature() ) {
			case BASIC: {
				processBasicCollectionElementTypeInformation(
						BasicPluralAttributeElementSource.class.cast( elementSource ),
						BasicPluralAttributeElementBinding.class.cast( pluralAttributeElementBinding )
				);
				break;
			}
			case COMPOSITE:
			case ONE_TO_MANY:
			case MANY_TO_MANY:
			case MANY_TO_ANY: {
				throw new UnsupportedOperationException( "Collection element nature not supported yet: " + pluralAttributeElementBinding
						.getPluralAttributeElementNature() );
			}
			default: {
				throw new AssertionFailure( "Unknown collection element nature : " + pluralAttributeElementBinding.getPluralAttributeElementNature() );
			}
		}
	}

	private void processBasicCollectionElementTypeInformation(
			BasicPluralAttributeElementSource elementSource,
			BasicPluralAttributeElementBinding basicCollectionElementBinding) {
		Type resolvedType = basicCollectionElementBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
		if ( resolvedType == null ) {
			bindHibernateTypeInformation(
					elementSource.getExplicitHibernateTypeSource(),
					basicCollectionElementBinding.getHibernateTypeDescriptor() );
			resolvedType = determineHibernateTypeFromDescriptor( basicCollectionElementBinding.getHibernateTypeDescriptor() );
		}
		if ( resolvedType != null ) {
			pushHibernateTypeInformationDown(
					basicCollectionElementBinding.getHibernateTypeDescriptor(),
					basicCollectionElementBinding.getRelationalValueBindings(),
					resolvedType
			);
		}
	}

	private static class ReflectedCollectionJavaTypes {
		private final Class<?> collectionType;
		private final Class<?> collectionElementType;
		private final Class<?> collectionIndexType;

		private ReflectedCollectionJavaTypes(
				Class<?> collectionType,
				Class<?> collectionElementType,
				Class<?> collectionIndexType) {
			this.collectionType = collectionType;
			this.collectionElementType = collectionElementType;
			this.collectionIndexType = collectionIndexType;
		}
	}

	/**
	 * @see HibernateTypeHelper#determineJavaType(PluralAttribute)
	 */
	private class PluralAttributeJavaTypeDeterminerDelegate implements BeanInfoHelper.BeanInfoDelegate {
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
				else {
				}
			}
			collectionJavaTypes = new ReflectedCollectionJavaTypes( collectionType, elementJavaType, indexJavaType );
		}

	}

}