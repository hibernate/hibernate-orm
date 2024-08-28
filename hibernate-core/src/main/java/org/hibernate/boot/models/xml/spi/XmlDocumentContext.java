/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAssociationAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.UserType;

import org.checkerframework.checker.nullness.qual.NonNull;

import static org.hibernate.internal.util.NullnessHelper.nullif;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Context for a specific XML mapping file
 *
 * @author Steve Ebersole
 */
public interface XmlDocumentContext {
	/**
	 * The XML document
	 */
	XmlDocument getXmlDocument();

	EffectiveMappingDefaults getEffectiveDefaults();

	/**
	 * Access to the containing SourceModelBuildingContext
	 */
	SourceModelBuildingContext getModelBuildingContext();

	/**
	 * Access to the containing BootstrapContext
	 */
	BootstrapContext getBootstrapContext();

	/**
	 * Resolve a ClassDetails by name, accounting for XML-defined package name if one.
	 */
	default MutableClassDetails resolveJavaType(String name) {
		try {
			return (MutableClassDetails) XmlAnnotationHelper.resolveJavaType( name, this );
		}
		catch (Exception e) {
			final HibernateException hibernateException = new HibernateException( "Unable to resolve Java type " + name );
			hibernateException.addSuppressed( e );
			throw hibernateException;
		}
	}

	/**
	 * Resolve a ClassDetails by name, accounting for XML-defined package name if one.
	 */
	default MutableClassDetails resolveDynamicJavaType(JaxbPersistentAttribute jaxbPersistentAttribute) {
		if ( jaxbPersistentAttribute instanceof JaxbBasicMapping jaxbBasicMapping ) {
			// <id/>, <basic/>, <tenant-id/>

			// explicit <target/>
			final String target = jaxbBasicMapping.getTarget();
			if ( isNotEmpty( target ) ) {
				return (MutableClassDetails) XmlAnnotationHelper.resolveJavaType( target, this );
			}

			// UserType
			final JaxbUserTypeImpl userTypeNode = jaxbBasicMapping.getType();
			if ( userTypeNode != null ) {
				final String userTypeImplName = userTypeNode.getValue();
				if ( isNotEmpty( userTypeImplName ) ) {
					final ClassDetails userTypeImplDetails = XmlAnnotationHelper.resolveJavaType( userTypeImplName, this );
					// safe to convert to class, though unfortunate to have to instantiate it...
					final UserType<?> userType = createInstance( userTypeImplDetails );
					final Class<?> modelClass = userType.returnedClass();
					return (MutableClassDetails) getModelBuildingContext().getClassDetailsRegistry().getClassDetails( modelClass.getName() );
				}
			}

			// JavaType
			final String javaTypeImplName = jaxbBasicMapping.getJavaType();
			if ( isNotEmpty( javaTypeImplName ) ) {
				final ClassDetails javaTypeImplDetails = XmlAnnotationHelper.resolveJavaType( javaTypeImplName, this );
				// safe to convert to class, though unfortunate to have to instantiate it...
				final JavaType<?> javaType = createInstance( javaTypeImplDetails );
				final Class<?> modelClass = javaType.getJavaTypeClass();
				return (MutableClassDetails) getModelBuildingContext().getClassDetailsRegistry().getClassDetails( modelClass.getName() );
			}

			// JdbcType
			final String jdbcTypeImplName = jaxbBasicMapping.getJdbcType();
			final Integer jdbcTypeCode = jaxbBasicMapping.getJdbcTypeCode();
			final JdbcType jdbcType;
			if ( isNotEmpty( jdbcTypeImplName ) ) {
				final ClassDetails jdbcTypeImplDetails = XmlAnnotationHelper.resolveJavaType( javaTypeImplName, this );
				jdbcType = createInstance( jdbcTypeImplDetails );
			}
			else if ( jdbcTypeCode != null ) {
				jdbcType = getBootstrapContext().getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( jdbcTypeCode );
			}
			else {
				jdbcType = null;
			}
			if ( jdbcType != null ) {
				final JavaType<?> javaType = jdbcType.getJdbcRecommendedJavaTypeMapping( 0, 0, getBootstrapContext().getTypeConfiguration() );
				final Class<?> modelClass = javaType.getJavaTypeClass();
				return (MutableClassDetails) getModelBuildingContext().getClassDetailsRegistry().getClassDetails( modelClass.getName() );
			}

			// fall through to exception
		}

		if ( jaxbPersistentAttribute instanceof JaxbEmbeddedMapping jaxbEmbeddedMapping ) {
			// <embedded/>, <embedded-id/>
			final String target = jaxbEmbeddedMapping.getTarget();
			if ( isNotEmpty( target ) ) {
				return (MutableClassDetails) getModelBuildingContext().getClassDetailsRegistry()
						.resolveClassDetails( target );
			}
			// fall through to exception
		}

		if ( jaxbPersistentAttribute instanceof JaxbAssociationAttribute jaxbAssociationAttribute ) {
			final String target = jaxbAssociationAttribute.getTargetEntity();
			if ( isNotEmpty( target ) ) {
				return (MutableClassDetails) getModelBuildingContext().getClassDetailsRegistry()
						.resolveClassDetails( target );
			}
			// fall through to exception
		}

		if ( jaxbPersistentAttribute instanceof JaxbAnyMapping ) {
			// todo : this is problematic because we'd really want Object, but the hibernate-models
			//  	definition of ClassDetails(Object) is immutable.  Probably the best option here
			//  	is to create a new (unregistered) DynamicClassDetails for each
			throw new UnsupportedOperationException( "Not yet implemented" );
		}

		if ( jaxbPersistentAttribute instanceof JaxbPluralAttribute jaxbPluralAttribute ) {
			final LimitedCollectionClassification classification = nullif( jaxbPluralAttribute.getClassification(), LimitedCollectionClassification.BAG );
			return switch ( classification ) {
				case BAG -> resolveJavaType( Collection.class.getName() );
				case LIST -> resolveJavaType( List.class.getName() );
				case SET -> resolveJavaType( Set.class.getName() );
				case MAP -> resolveJavaType( Map.class.getName() );
			};
		}

		// todo : would be nice to have at least the XML origin (file name, etc) for the exception.
		//		the "context" (class where this happens) would be even more nicerer
		throw new HibernateException( "Could not determine target type for dynamic attribute - " + jaxbPersistentAttribute.getName() );
	}

	@NonNull
	private <T> T createInstance(ClassDetails classDetails) {
		try {
			//noinspection unchecked
			return (T) classDetails.toJavaClass().getConstructor().newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to create instance from incoming ClassDetails - " + classDetails );
		}
	}

	default String resolveClassName(String specifiedName) {
		if ( specifiedName.contains( "." ) ) {
			return specifiedName;
		}

		return StringHelper.qualifyConditionallyIfNot(
				getXmlDocument().getDefaults().getPackage(),
				specifiedName
		);
	}
}
