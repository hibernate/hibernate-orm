/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmSingularPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddableDomainType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;

import org.jboss.logging.Logger;

import static java.util.Collections.emptyList;

/**
 * Functionality common to all implementations of {@link IdentifiableType}.
 * <p>
 * An identifiable type is one which may have an identifier attribute, that
 * is, an {@linkplain jakarta.persistence.Entity entity type} or a
 * {@linkplain jakarta.persistence.MappedSuperclass mapped superclass}.
 *
 * @apiNote Currently we only really have support for direct entities in the
 *          Hibernate metamodel as the information for them is consumed into
 *          the closest actual entity subclass(es) in the internal Hibernate
 *          mapping metamodel.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableType<J>
		extends AbstractManagedType<J>
		implements IdentifiableDomainType<J>, Serializable {

	private final boolean hasIdentifierProperty;
	private final boolean hasIdClass;

	private SqmSingularPersistentAttribute<J,?> id;
	private List<SqmSingularPersistentAttribute<? super J,?>> nonAggregatedIdAttributes;
	private SqmEmbeddableDomainType<?> idClassType;

	private SqmPathSource<?> identifierDescriptor;

	private final boolean isVersioned;
	private SqmSingularPersistentAttribute<J, ?> versionAttribute;
	private List<SqmPersistentAttribute<J,?>> naturalIdAttributes;

	public AbstractIdentifiableType(
			String typeName,
			JavaType<J> javaType,
			IdentifiableDomainType<? super J> superType,
			boolean hasIdClass,
			boolean hasIdentifierProperty,
			boolean versioned,
			JpaMetamodelImplementor metamodel) {
		super( typeName, javaType, superType, metamodel );
		this.hasIdClass = hasIdClass;
		this.hasIdentifierProperty = hasIdentifierProperty;
		this.isVersioned = versioned;
	}

	@Override
	protected InFlightAccess<J> createInFlightAccess() {
		return new InFlightAccessImpl( super.createInFlightAccess() );
	}

	@Override
	public SqmPathSource<?> getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	public boolean hasIdClass() {
		return hasIdClass;
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return !hasIdClass() && hasIdentifierProperty;
	}

	@Override
	public AbstractIdentifiableType<? super J> getSuperType() {
		// overridden simply to perform the cast
		return (AbstractIdentifiableType<? super J>) super.getSuperType();
	}

	@Override
	public IdentifiableDomainType<? super J> getSupertype() {
		return getSuperType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SqmSingularPersistentAttribute<? super J, Y> getId(Class<Y> javaType) {
		ensureNoIdClass();
		final var id = findIdAttribute();
		if ( id != null ) {
			checkType( id, javaType );
		}
		return (SqmSingularPersistentAttribute<? super J, Y>) id;
	}

	private void ensureNoIdClass() {
		if ( hasIdClass() ) {
			throw new IllegalArgumentException(
					"Illegal call to IdentifiableType#getId for class [" + getTypeName() + "] defined with @IdClass"
			);
		}
	}


	@Override
	public SqmSingularPersistentAttribute<? super J, ?> findIdAttribute() {
		if ( id != null ) {
			return id;
		}
		else if ( getSuperType() != null ) {
			return getSuperType().findIdAttribute();
		}
		else {
			return null;
		}
	}

	private void checkType(SingularPersistentAttribute<?, ?> attribute, Class<?> javaType) {
		if ( !javaType.isAssignableFrom( attribute.getType().getJavaType() ) ) {
			final JavaType<?> attributeJavaType = attribute.getAttributeJavaType();
			if ( !( attributeJavaType instanceof PrimitiveJavaType<?> primitiveJavaType )
					|| primitiveJavaType.getPrimitiveClass() != javaType ) {
				throw new IllegalArgumentException(
						String.format(
								"Attribute [%s#%s : %s] not castable to requested type [%s]",
								getTypeName(),
								attribute.getName(),
								attribute.getType().getJavaType().getName(),
								javaType.getName()
						)
				);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SqmSingularPersistentAttribute<J, Y> getDeclaredId(Class<Y> javaType) {
		ensureNoIdClass();
		if ( id == null ) {
			throw new IllegalArgumentException( "The id attribute is not declared on this type [" + getTypeName() + "]" );
		}
		checkType( id, javaType );
		return (SqmSingularPersistentAttribute<J, Y>) id;
	}

	@Override
	public SimpleDomainType<?> getIdType() {
		final var id = findIdAttribute();
		if ( id != null ) {
			return id.getType();
		}
		else {
			final var idClassAttributes = getIdClassAttributesSafely();
			if ( idClassAttributes != null ) {
				if ( idClassAttributes.size() == 1 ) {
					return idClassAttributes.iterator().next().getType();
				}
				else if ( idClassType instanceof SimpleDomainType<?> simpleDomainType ) {
					return simpleDomainType;
				}
			}
			return null;
		}
	}

	/**
	 * A form of {@link #getIdClassAttributes} which prefers to return {@code null} rather than throw exceptions
	 *
	 * @return IdClass attributes or {@code null}
	 */
	public Set<SingularPersistentAttribute<? super J, ?>> getIdClassAttributesSafely() {
		if ( hasIdClass() ) {
			final Set<SingularPersistentAttribute<? super J, ?>> attributes = new HashSet<>();
			visitIdClassAttributes( attributes::add );
			return attributes.isEmpty() ? null : attributes;
		}
		else {
			return null;
		}
	}

	@Override
	public Set<SingularAttribute<? super J, ?>> getIdClassAttributes() {
		if ( !hasIdClass() ) {
			throw new IllegalArgumentException( "This class [" + getJavaType() + "] does not define an IdClass" );
		}

		final Set<SingularAttribute<? super J, ?>> attributes = new HashSet<>();
		visitIdClassAttributes( attributes::add );
		if ( attributes.isEmpty() ) {
			throw new IllegalArgumentException( "Unable to locate IdClass attributes [" + getJavaType() + "]" );
		}
		return attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitIdClassAttributes(Consumer<SingularPersistentAttribute<? super J, ?>> attributeConsumer) {
		if ( nonAggregatedIdAttributes != null ) {
			nonAggregatedIdAttributes.forEach( attributeConsumer );
		}
		else if ( getSuperType() != null ) {
			//noinspection rawtypes
			getSuperType().visitIdClassAttributes( (Consumer) attributeConsumer );
		}
	}

	@Override
	public boolean hasVersionAttribute() {
		return isVersioned;
	}

	public boolean hasDeclaredVersionAttribute() {
		return isVersioned && versionAttribute != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SingularPersistentAttribute<? super J, Y> getVersion(Class<Y> javaType) {
		if ( hasVersionAttribute() ) {
			final var version = findVersionAttribute();
			if ( version != null ) {
				checkType( version, javaType );
			}
			return (SingularPersistentAttribute<? super J, Y>) version;
		}
		else {
			return null;
		}
	}

	@Override
	public SqmSingularPersistentAttribute<? super J, ?> findVersionAttribute() {
		if ( versionAttribute != null ) {
			return versionAttribute;
		}
		else if ( getSuperType() != null ) {
			return getSuperType().findVersionAttribute();
		}
		else {
			return null;
		}
	}

	@Override
	public List<? extends PersistentAttribute<? super J, ?>> findNaturalIdAttributes() {
		if ( naturalIdAttributes != null ) {
			return naturalIdAttributes;
		}
		else if ( getSuperType() != null ) {
			return getSuperType().findNaturalIdAttributes();
		}
		else {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SingularPersistentAttribute<J, Y> getDeclaredVersion(Class<Y> javaType) {
		checkDeclaredVersion();
		checkType( versionAttribute, javaType );
		return (SingularPersistentAttribute<J, Y>) versionAttribute;
	}

	private void checkDeclaredVersion() {
		if ( versionAttribute == null || ( getSuperType() != null && getSuperType().hasVersionAttribute() )) {
			throw new IllegalArgumentException(
					"The version attribute is not declared by this type [" + getJavaType() + "]"
			);
		}
	}

//	@Override
//	public void visitJdbcTypes(Consumer action, TypeConfiguration typeConfiguration) {
//		id.visitJdbcTypes( action, typeConfiguration );
//
//		if ( versionAttribute != null ) {
//			versionAttribute.visitJdbcTypes( action, typeConfiguration );
//		}
//
//		visitAttributes(
//				attribute -> attribute.visitJdbcTypes( action, typeConfiguration )
//		);
//	}

	/**
	 * For used to retrieve the declared version when populating the static metamodel.
	 *
	 * @return The declared
	 */
	public SqmSingularPersistentAttribute<J, ?> getDeclaredVersion() {
		checkDeclaredVersion();
		return versionAttribute;
	}

	private class InFlightAccessImpl extends AbstractManagedType<J>.InFlightAccessImpl {
		private final InFlightAccess<J> managedTypeAccess;

		private InFlightAccessImpl(InFlightAccess<J> managedTypeAccess) {
			this.managedTypeAccess = managedTypeAccess;
		}

		@Override
		public void applyIdAttribute(SingularPersistentAttribute<J, ?> idAttribute) {
			id = (SqmSingularPersistentAttribute<J, ?>) idAttribute;
			managedTypeAccess.addAttribute( idAttribute );
		}

		@Override
		public void applyNonAggregatedIdAttributes(
				Set<SingularPersistentAttribute<? super J, ?>> idAttributes,
				EmbeddableDomainType<?> idClassType) {
			if ( id != null ) {
				throw new IllegalArgumentException( "`AbstractIdentifiableType#id` already set on call to `#applyNonAggregatedIdAttribute`" );
			}

			if ( nonAggregatedIdAttributes != null ) {
				throw new IllegalStateException( "Non-aggregated id attributes were already set" );
			}

			if ( idAttributes.isEmpty() ) {
				nonAggregatedIdAttributes = emptyList();
			}
			else {
				nonAggregatedIdAttributes = new ArrayList<>(idAttributes.size());
				for ( var idAttribute : idAttributes ) {
					nonAggregatedIdAttributes.add( (SqmSingularPersistentAttribute<? super J, ?>) idAttribute );
					if ( AbstractIdentifiableType.this == idAttribute.getDeclaringType() ) {
						@SuppressWarnings("unchecked")
						// Safe, because we know it's declared  by this type
						final PersistentAttribute<J, ?> declaredAttribute =
								(PersistentAttribute<J, ?>) idAttribute;
						addAttribute( declaredAttribute );
					}
				}
			}
			AbstractIdentifiableType.this.idClassType = (SqmEmbeddableDomainType<?>) idClassType;
		}

		@Override
		public void applyIdClassAttributes(Set<SingularPersistentAttribute<? super J, ?>> idClassAttributes) {
			applyNonAggregatedIdAttributes( idClassAttributes, null );
		}

		@Override
		public void applyVersionAttribute(SingularPersistentAttribute<J, ?> versionAttribute) {
			AbstractIdentifiableType.this.versionAttribute =
					(SqmSingularPersistentAttribute<J, ?>) versionAttribute;
			managedTypeAccess.addAttribute( versionAttribute );
		}

		@Override
		public void applyNaturalIdAttribute(PersistentAttribute<J, ?> naturalIdAttribute) {
			if ( naturalIdAttributes == null ) {
				naturalIdAttributes = new ArrayList<>();
			}
			naturalIdAttributes.add( (SqmPersistentAttribute<J, ?>) naturalIdAttribute );
		}

		@Override
		public void addAttribute(PersistentAttribute<J, ?> attribute) {
			managedTypeAccess.addAttribute( attribute );
		}

		@Override
		public void finishUp() {
			managedTypeAccess.finishUp();
			identifierDescriptor = interpretIdDescriptor();
		}
	}

	private static final Logger log = Logger.getLogger( AbstractIdentifiableType.class );

	private SqmPathSource<?> interpretIdDescriptor() {
		log.tracef( "Interpreting domain-model identifier descriptor" );

		final var superType = getSuperType();
		if ( superType != null ) {
			final var idDescriptor = superType.getIdentifierDescriptor();
			if ( idDescriptor != null ) {
				return idDescriptor;
			}
		}

		if ( id != null ) {
			// simple id or aggregate composite id
			return pathSource( id );
		}
		else if ( nonAggregatedIdAttributes != null && !nonAggregatedIdAttributes.isEmpty() ) {
			return compositePathSource();
		}
		else {
			if ( isIdMappingRequired() ) {
				throw new UnsupportedMappingException(
						"Could not build SqmPathSource for entity identifier : " + getTypeName() );
			}
			return null;
		}

	}

	private AbstractSqmPathSource<?> compositePathSource() {
		// non-aggregate composite id
		if ( idClassType == null ) {
			return new NonAggregatedCompositeSqmPathSource<>(
					EntityIdentifierMapping.ID_ROLE_NAME,
					null,
					Bindable.BindableType.SINGULAR_ATTRIBUTE,
					this
			);
		}
		else {
			return new EmbeddedSqmPathSource<>(
					EntityIdentifierMapping.ID_ROLE_NAME,
					null,
					idClassType,
					Bindable.BindableType.SINGULAR_ATTRIBUTE,
					false
			);
		}
	}

	private <T> AbstractSqmPathSource<T> pathSource(SqmSingularPersistentAttribute<J,T> attribute) {
		final DomainType<T> type = attribute.getType();
		if ( type instanceof BasicDomainType<T> basicDomainType ) {
			return new BasicSqmPathSource<>(
					EntityIdentifierMapping.ID_ROLE_NAME,
					attribute,
					basicDomainType,
					type.getExpressibleJavaType(),
					Bindable.BindableType.SINGULAR_ATTRIBUTE,
					attribute.isGeneric()
			);
		}
		else if ( type instanceof SqmEmbeddableDomainType<T> embeddableDomainType ) {
			return new EmbeddedSqmPathSource<>(
					EntityIdentifierMapping.ID_ROLE_NAME,
					attribute,
					embeddableDomainType,
					Bindable.BindableType.SINGULAR_ATTRIBUTE,
					attribute.isGeneric()
			);
		}
		else if (type instanceof BasicSqmPathSource<T> pathSource) {
			return pathSource;
		}
		else {
			throw new AssertionFailure( "Unrecognized type: " + type );
		}
	}

	protected boolean isIdMappingRequired() {
		return true;
	}
}
