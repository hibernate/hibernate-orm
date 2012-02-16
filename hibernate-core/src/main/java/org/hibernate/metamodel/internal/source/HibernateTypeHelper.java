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
import org.hibernate.metamodel.spi.binding.CompositionAttributeBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.AbstractValue;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * Serves 2 roles:<ol>
 *     <li>
 *         Takes information about an attribute mapping and determines the appropriate Hibernate
 *         {@link org.hibernate.type.Type} to use, if possible.
 *     </li>
 *     <li>
 *         Given a Hibernate {@link org.hibernate.type.Type}, it pushes the jdbc and java type information
 *         reported the {@link org.hibernate.type.Type} into parts of the metamodel that may be missing it
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
 */
public class HibernateTypeHelper {
	private static final Logger log = Logger.getLogger( HibernateTypeHelper.class );

	private final Binder binder;

	public HibernateTypeHelper(Binder binder) {
		this.binder = binder;
	}

	private org.hibernate.metamodel.spi.domain.Type makeJavaType(String name) {
		return binder.getCurrentBindingContext().makeJavaType( name );
	}

	private MetadataImplementor metadata() {
		return binder.getMetadata();
	}

	public void bindSingularAttributeTypeInformation(
			ExplicitHibernateTypeSource typeSource,
			SingularAttributeBinding attributeBinding) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();

		final Class<?> attributeJavaType = determineJavaType( attributeBinding.getAttribute() );
		if ( attributeJavaType != null ) {
			attributeBinding.getAttribute().resolveType( makeJavaType( attributeJavaType.getName() ) );
			if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
				hibernateTypeDescriptor.setJavaTypeName( attributeJavaType.getName() );
			}
		}

		bindHibernateTypeInformation( typeSource, hibernateTypeDescriptor );

		processSingularAttributeTypeInformation( attributeBinding );
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

		//todo : we need to support type descriptors at multiple levels
		bindHibernateTypeInformation( attributeSource.getTypeInformation(), attributeBinding.getHibernateTypeDescriptor() );
		processPluralAttributeTypeInformation( attributeBinding );
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
			final TypeDefinition typeDefinition = metadata().getTypeDefinition( explicitTypeName );
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
	 * @param attributeBinding The attribute.
	 */
	private void processSingularAttributeTypeInformation(SingularAttributeBinding attributeBinding) {
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
			pushHibernateTypeInformationDown( attributeBinding, resolvedType );
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
				return metadata().getTypeResolver().heuristicType( typeName, typeParameters );
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
			SingularAttributeBinding attributeBinding,
			Type resolvedHibernateType) {

		// sql type information ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( BasicAttributeBinding.class.isInstance( attributeBinding ) ) {
			pushHibernateTypeInformationDown(
					(BasicAttributeBinding) attributeBinding,
					resolvedHibernateType
			);
		}
		else if ( CompositionAttributeBinding.class.isInstance( attributeBinding ) ) {
			pushHibernateTypeInformationDown(
					(CompositionAttributeBinding) attributeBinding,
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
			CompositionAttributeBinding attributeBinding,
			Type resolvedHibernateType) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		final SingularAttribute singularAttribute = SingularAttribute.class.cast( attributeBinding.getAttribute() );
		if ( ! singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeName() != null ) {
			singularAttribute.resolveType( makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		}

		for ( AttributeBinding subAttributeBinding : attributeBinding.attributeBindings() ) {
			if ( SingularAttributeBinding.class.isInstance( subAttributeBinding ) ) {
				processSingularAttributeTypeInformation(
						SingularAttributeBinding.class.cast( subAttributeBinding )
				);
			}
			else if ( AbstractPluralAttributeBinding.class.isInstance( subAttributeBinding ) ) {
				processPluralAttributeTypeInformation(
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
								resolvedHibernateType.sqlTypes( metadata() )[0],
								resolvedHibernateType.getName(),
								resolvedHibernateType.getReturnedClass()
						)
				);
			}
		}
	}

	private void processPluralAttributeTypeInformation(PluralAttributeBinding attributeBinding) {
		if ( attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() != null ) {
			return;
		}

		Type resolvedType;
		// do NOT look at java type...
		//String typeName = determineTypeName( attributeBinding.getHibernateTypeDescriptor() );
		String typeName = attributeBinding.getHibernateTypeDescriptor().getExplicitTypeName();
		if ( typeName != null ) {
			resolvedType =
					metadata().getTypeResolver()
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
// todo : what exactly is getting pushed down here?  and to what/where?
//			pushHibernateTypeInformationDownIfNeeded(
//					attributeBinding.getHibernateTypeDescriptor(),
//					null,
//					resolvedType
//			);
		}
		bindCollectionElementTypeInformation( attributeBinding.getPluralAttributeElementBinding() );
	}

	private Type determineHibernateTypeFromCollectionType(PluralAttributeBinding attributeBinding) {
		final TypeFactory typeFactory = metadata().getTypeResolver().getTypeFactory();
		switch ( attributeBinding.getAttribute().getNature() ) {
			case SET: {
				return typeFactory.set(
						attributeBinding.getAttribute().getName(),
						attributeBinding.getReferencedPropertyName(),
						attributeBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() == PluralAttributeElementNature.COMPOSITE
				);
			}
			case BAG: {
				return typeFactory.bag(
						attributeBinding.getAttribute().getName(),
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

	private void bindCollectionElementTypeInformation(PluralAttributeElementBinding pluralAttributeElementBinding) {
		switch ( pluralAttributeElementBinding.getPluralAttributeElementNature() ) {
			case BASIC: {
				bindBasicCollectionElementTypeInformation(
						BasicPluralAttributeElementBinding.class.cast(
								pluralAttributeElementBinding
						)
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

	private void bindBasicCollectionElementTypeInformation(BasicPluralAttributeElementBinding basicCollectionElement) {
		Type resolvedHibernateType = determineHibernateTypeFromDescriptor( basicCollectionElement.getHibernateTypeDescriptor() );
		if ( resolvedHibernateType != null ) {
			pushHibernateTypeInformationDown(
					basicCollectionElement.getHibernateTypeDescriptor(),
					basicCollectionElement.getRelationalValueBindings(),
					resolvedHibernateType
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
