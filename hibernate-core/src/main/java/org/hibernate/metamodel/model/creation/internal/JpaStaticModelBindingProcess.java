/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.internal;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Type;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationProcess;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierComposite;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.InheritanceCapable;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.spi.MetamodelImplementor;

/**
 * @author Steve Ebersole
 */
public class JpaStaticModelBindingProcess {
	public static void processBindings(MetamodelImplementor completedDomainModel, JpaStaticMetaModelPopulationSetting support) {
		new JpaStaticModelBindingProcess( support ).bindJpaStaticMetaModel( completedDomainModel );
	}

	private final JpaStaticMetaModelPopulationSetting support;
	private final Set<Class<?>> processedMetamodelClasses = new HashSet<>();

	private JpaStaticModelBindingProcess(JpaStaticMetaModelPopulationSetting support) {
		this.support = support;
	}



	private void bindJpaStaticMetaModel(MetamodelImplementor completedDomainModel) {
		completedDomainModel.visitEntityHierarchies(
				hierarchy -> {
					processSupers( hierarchy.getRootEntityType() );
					processManagedType( hierarchy.getRootEntityType() );
					processSubs( hierarchy.getRootEntityType() );
				}
		);

		completedDomainModel.visitEmbeddedDescriptors(
				embeddedDescriptor -> {
					// NOTE : process supers/subs for the embeddable/embedded for future
					// expansion.  embeddables implement inheritance capability, we just
					// do not leverage that yet
					processSupers( embeddedDescriptor );
					processManagedType( embeddedDescriptor );
					processSubs( embeddedDescriptor );
				}
		);
	}

	private void processSupers(InheritanceCapable<?> domainType) {
		if ( domainType == null ) {
			return;
		}

		InheritanceCapable<?> superclassType = domainType.getSuperclassType();
		while ( superclassType != null ) {
			processSupers( superclassType );
			processManagedType( superclassType );

			superclassType = superclassType.getSuperclassType();
		}
	}

	private void processSubs(InheritanceCapable<?> domainType) {
		for ( InheritanceCapable<?> subclassType : domainType.getSubclassTypes() ) {
			processManagedType( subclassType );
			processSubs( subclassType );
		}
	}

	private <X> void processManagedType(ManagedTypeDescriptor<X> managedType) {
		final Class<X> managedTypeClass = managedType.getJavaType();
		if ( managedTypeClass == null ) {
			// should indicate MAP entity mode, skip...
			return;
		}

		final String metamodelClassName = managedTypeClass.getName() + '_';
		try {
			final Class metamodelClass = Class.forName( metamodelClassName, true, managedTypeClass.getClassLoader() );
			// we found the class; so populate it...
			bindJpaStaticModelAttributes( metamodelClass, managedType );
		}
		catch (ClassNotFoundException ignore) {
			// nothing to do...
		}
	}


	private <X> void bindJpaStaticModelAttributes(
			Class<?> staticModelClass,
			ManagedTypeDescriptor<X> managedType) {
		if ( ! processedMetamodelClasses.add( staticModelClass ) ) {
			return;
		}

		for ( NonIdPersistentAttribute persistentAttribute : managedType.getDeclaredPersistentAttributes() ) {
			bindJpaStaticModelAttribute( staticModelClass, persistentAttribute );
		}

		if ( managedType instanceof IdentifiableTypeDescriptor ) {
			final IdentifiableTypeDescriptor<X> entityType = (IdentifiableTypeDescriptor<X>) managedType;
			if ( entityType == entityType.getHierarchy().getRootEntityType() ) {
				// handle version
				if ( entityType.getHierarchy().getVersionDescriptor() != null ) {
					bindJpaStaticModelAttribute( staticModelClass, entityType.getHierarchy().getVersionDescriptor() );
				}

				// handle id
				if ( entityType.getHierarchy().getIdentifierDescriptor() instanceof EntityIdentifierSimple ) {
					bindJpaStaticModelAttribute(
							staticModelClass,
							(EntityIdentifierSimple) entityType.getHierarchy().getIdentifierDescriptor()
					);
				}
				else {
					final EntityIdentifierComposite identifierDescriptor = (EntityIdentifierComposite) entityType.getHierarchy()
							.getIdentifierDescriptor();
					if ( identifierDescriptor instanceof EntityIdentifierCompositeAggregated ) {
						processManagedType( identifierDescriptor.getEmbeddedDescriptor() );
					}
					else {
						// todo (6.0) : not yet implemented
					}
				}
			}
		}
	}

	private static final EntityManagerMessageLogger LOG = HEMLogging.messageLogger( RuntimeModelCreationProcess.class );

	private void bindJpaStaticModelAttribute(Class<?> staticModelClass, PersistentAttribute persistentAttribute) {
		final String name = persistentAttribute.getName();
		try {
			// there is a shortcoming in the existing Hibernate code in terms of the way MappedSuperclass
			// support was bolted on which comes to bear right here when the attribute is an embeddable type
			// defined on a MappedSuperclass.  We do not have the correct information to determine the
			// appropriate attribute declarer in such cases and so the incoming metamodelClass most likely
			// does not represent the declarer in such cases.
			//
			// As a result, in the case of embeddable classes we simply use getField rather than get
			// getDeclaredField
			final boolean allowNonDeclaredFieldReference =
					persistentAttribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED
							|| persistentAttribute.getDeclaringType().getPersistenceType() == Type.PersistenceType.EMBEDDABLE;

			final Field field = allowNonDeclaredFieldReference
					? staticModelClass.getField( name )
					: staticModelClass.getDeclaredField( name );
			try {
				// should be public anyway, but to be sure...
				ReflectHelper.ensureAccessibility( field );
				field.set( null, persistentAttribute );
			}
			catch (IllegalAccessException e) {
				// todo : exception type?
				throw new AssertionFailure(
						"Unable to inject static metamodel attribute : " + staticModelClass.getName() + '#' + name,
						e
				);
			}
			catch (IllegalArgumentException e) {
				// most likely a mismatch in the type we are injecting and the defined field; this represents a
				// mismatch in how the annotation processor interpretted the attribute and how our metamodel
				// and/or annotation binder did.

//              This is particularly the case as arrays are nto handled propery by the StaticMetamodel generator

//				throw new AssertionFailure(
//						"Illegal argument on static metamodel field injection : " + metamodelClass.getName() + '#' + name
//								+ "; expected type :  " + attribute.getClass().getName()
//								+ "; encountered type : " + field.getType().getName()
//				);
				LOG.illegalArgumentOnStaticMetamodelFieldInjection(
						staticModelClass.getName(),
						name,
						persistentAttribute.getClass().getName(),
						field.getType().getName()
				);
			}
		}
		catch (NoSuchFieldException e) {
			LOG.unableToLocateStaticMetamodelField( staticModelClass.getName(), name );
//			throw new AssertionFailure(
//					"Unable to locate static metamodel field : " + metamodelClass.getName() + '#' + name
//			);
		}
	}
}
