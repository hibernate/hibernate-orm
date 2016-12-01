/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeConverter;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.annotations.HCANNHelper;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.classmate.members.ResolvedMethod;

/**
 * The standard AttributeConverterDescriptor implementation
 *
 * @author Steve Ebersole
 */
public class AttributeConverterDescriptorImpl implements AttributeConverterDescriptor {
	private final AttributeConverter attributeConverter;
	private final boolean autoApply;
	private final ResolvedType domainType;
	private final ResolvedType jdbcType;


	public static AttributeConverterDescriptor create(
			AttributeConverterDefinition definition,
			ClassmateContext classmateContext) {
		final AttributeConverter converter = definition.getAttributeConverter();
		final Class converterClass = converter.getClass();

		final ResolvedType converterType = classmateContext.getTypeResolver().resolve( converterClass );
		final List<ResolvedType> converterParamTypes = converterType.typeParametersFor( AttributeConverter.class );
		if ( converterParamTypes == null ) {
			throw new AnnotationException(
					"Could not extract type parameter information from AttributeConverter implementation ["
							+ converterClass.getName() + "]"
			);
		}
		else if ( converterParamTypes.size() != 2 ) {
			throw new AnnotationException(
					"Unexpected type parameter information for AttributeConverter implementation [" +
							converterClass.getName() + "]; expected 2 parameter types, but found " + converterParamTypes.size()
			);
		}

		return new AttributeConverterDescriptorImpl(
				converter,
				definition.isAutoApply(),
				converterParamTypes.get( 0 ),
				converterParamTypes.get( 1 )
		);
	}

	private AttributeConverterDescriptorImpl(
			AttributeConverter attributeConverter,
			boolean autoApply,
			ResolvedType domainType,
			ResolvedType jdbcType) {
		this.attributeConverter = attributeConverter;
		this.autoApply = autoApply;
		this.domainType = domainType;
		this.jdbcType = jdbcType;
	}

	@Override
	public AttributeConverter getAttributeConverter() {
		return attributeConverter;
	}

	@Override
	public Class<?> getDomainType() {
		return domainType.getErasedType();
	}

	@Override
	public Class<?> getJdbcType() {
		return jdbcType.getErasedType();
	}

	@Override
	@SuppressWarnings("SimplifiableIfStatement")
	public boolean shouldAutoApplyToAttribute(XProperty xProperty, MetadataBuildingContext context) {
		if ( !autoApply ) {
			return false;
		}

		final ResolvedType attributeType = resolveAttributeType( xProperty, context );
		return typesMatch( domainType, attributeType );
	}

	private ResolvedType resolveAttributeType(XProperty xProperty, MetadataBuildingContext context) {
		return resolveMember( xProperty, context ).getType();
	}

	private ResolvedMember resolveMember(XProperty xProperty, MetadataBuildingContext buildingContext) {
		final ClassmateContext classmateContext = buildingContext.getMetadataCollector().getClassmateContext();
		final ReflectionManager reflectionManager = buildingContext.getBuildingOptions().getReflectionManager();

		final ResolvedType declaringClassType = classmateContext.getTypeResolver().resolve(
				reflectionManager.toClass( xProperty.getDeclaringClass() )
		);
		final ResolvedTypeWithMembers declaringClassWithMembers = classmateContext.getMemberResolver().resolve(
				declaringClassType,
				null,
				null
		);

		final Member member = toMember( xProperty );
		if ( member instanceof Method ) {
			for ( ResolvedMethod resolvedMember : declaringClassWithMembers.getMemberMethods() ) {
				if ( resolvedMember.getName().equals( member.getName() ) ) {
					return resolvedMember;
				}
			}
		}
		else if ( member instanceof Field ) {
			for ( ResolvedField resolvedMember : declaringClassWithMembers.getMemberFields() ) {
				if ( resolvedMember.getName().equals( member.getName() ) ) {
					return resolvedMember;
				}
			}
		}
		else {
			throw new HibernateException( "Unexpected java.lang.reflect.Member type from org.hibernate.annotations.common.reflection.java.JavaXMember : " + member );
		}

		throw new HibernateException(
				"Could not locate resolved type information for attribute [" + member.getName() + "] from Classmate"
		);
	}


	private static Member toMember(XProperty xProperty) {
		try {
			return HCANNHelper.getUnderlyingMember( xProperty );
		}
		catch (Exception e) {
			throw new HibernateException(
					"Could not resolve member signature from XProperty reference",
					e
			);
		}
	}

	private boolean typesMatch(ResolvedType converterDefinedType, ResolvedType checkType) {
		if ( !converterDefinedType.getErasedType().isAssignableFrom( checkType.getErasedType() ) ) {
			return false;
		}

		// if the converter did not define any nested type parameters, then the check above is
		// enough for a match
		if ( converterDefinedType.getTypeParameters().isEmpty() ) {
			return true;
		}

		// however, here the converter *did* define nested type parameters, so we'd have a converter defined using something like, e.g., List<String> for its
		// domain type.
		//
		// we need to check those nested types as well

		if ( checkType.getTypeParameters().isEmpty() ) {
			// the domain type did not define nested type params.  a List<String> would not auto-match a List(<Object>)
			return false;
		}

		if ( converterDefinedType.getTypeParameters().size() != checkType.getTypeParameters().size() ) {
			// they had different number of type params somehow.
			return false;
		}

		for ( int i = 0; i < converterDefinedType.getTypeParameters().size(); i++ ) {
			if ( !typesMatch( converterDefinedType.getTypeParameters().get( i ), checkType.getTypeParameters().get( i ) ) ) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean shouldAutoApplyToCollectionElement(XProperty xProperty, MetadataBuildingContext context) {
		if ( !autoApply ) {
			return false;
		}

		final ResolvedMember collectionMember = resolveMember( xProperty, context );
		final ResolvedType elementType;

		if ( Map.class.isAssignableFrom( collectionMember.getType().getErasedType() ) ) {
			elementType = collectionMember.getType().typeParametersFor( Map.class ).get( 1 );
		}
		else if ( Collection.class.isAssignableFrom( collectionMember.getType().getErasedType() ) ) {
			elementType = collectionMember.getType().typeParametersFor( Collection.class ).get( 0 );
		}
		else {
			throw new HibernateException( "Attribute was neither a Collection nor a Map : " + collectionMember.getType().getErasedType() );
		}

		return typesMatch( domainType, elementType );
	}

	@Override
	public boolean shouldAutoApplyToMapKey(XProperty xProperty, MetadataBuildingContext context) {
		if ( !autoApply ) {
			return false;
		}

		final ResolvedMember collectionMember = resolveMember( xProperty, context );
		final ResolvedType keyType;

		if ( Map.class.isAssignableFrom( collectionMember.getType().getErasedType() ) ) {
			keyType = collectionMember.getType().typeParametersFor( Map.class ).get( 0 );
		}
		else {
			throw new HibernateException( "Attribute was not a Map : " + collectionMember.getType().getErasedType() );
		}

		return typesMatch( domainType, keyType );
	}
}
