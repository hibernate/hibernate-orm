/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.NonAggregatedCompositeSqmPathSource;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;

import org.jboss.logging.Logger;

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

	private SingularPersistentAttribute<J,?> id;
	private Set<SingularPersistentAttribute<? super J,?>> nonAggregatedIdAttributes;
	private EmbeddableDomainType<?> idClassType;

	private SqmPathSource<?> identifierDescriptor;


	private final boolean isVersioned;
	private SingularPersistentAttribute<J, ?> versionAttribute;
	private List<PersistentAttribute<J,?>> naturalIdAttributes;

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
	protected InFlightAccessImpl createInFlightAccess() {
		return new InFlightAccessImpl( super.createInFlightAccess() );
	}

	@Override
	public InFlightAccessImpl getInFlightAccess() {
		return (InFlightAccessImpl) super.getInFlightAccess();
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
	public IdentifiableDomainType<? super J> getSuperType() {
		// overridden simply to perform the cast
		return (IdentifiableDomainType<? super J>) super.getSuperType();
	}

	@Override
	public IdentifiableDomainType<? super J> getSupertype() {
		return getSuperType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SingularPersistentAttribute<? super J, Y> getId(Class<Y> javaType) {
		ensureNoIdClass();
		SingularPersistentAttribute<? super J, ?> id = findIdAttribute();
		if ( id != null ) {
			checkType( id, javaType );
		}
		return (SingularPersistentAttribute<? super J, Y>) id;
	}

	private void ensureNoIdClass() {
		if ( hasIdClass() ) {
			throw new IllegalArgumentException(
					"Illegal call to IdentifiableType#getId for class [" + getTypeName() + "] defined with @IdClass"
			);
		}
	}


	@Override
	public SingularPersistentAttribute<? super J, ?> findIdAttribute() {
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
			if ( !( attributeJavaType instanceof PrimitiveJavaType )
					|| ( (PrimitiveJavaType<?>) attributeJavaType ).getPrimitiveClass() != javaType ) {
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
	public <Y> SingularPersistentAttribute<J, Y> getDeclaredId(Class<Y> javaType) {
		ensureNoIdClass();
		if ( id == null ) {
			throw new IllegalArgumentException( "The id attribute is not declared on this type [" + getTypeName() + "]" );
		}
		checkType( id, javaType );
		return (SingularPersistentAttribute<J, Y>) id;
	}

	@Override
	public SimpleDomainType<?> getIdType() {
		final SingularPersistentAttribute<? super J, ?> id = findIdAttribute();
		if ( id != null ) {
			return id.getType();
		}

		Set<SingularPersistentAttribute<? super J, ?>> idClassAttributes = getIdClassAttributesSafely();
		if ( idClassAttributes != null ) {
			if ( idClassAttributes.size() == 1 ) {
				return idClassAttributes.iterator().next().getType();
			}
		}

		return null;
	}

	/**
	 * A form of {@link #getIdClassAttributes} which prefers to return {@code null} rather than throw exceptions
	 *
	 * @return IdClass attributes or {@code null}
	 */
	public Set<SingularPersistentAttribute<? super J, ?>> getIdClassAttributesSafely() {
		if ( !hasIdClass() ) {
			return null;
		}
		final Set<SingularPersistentAttribute<? super J,?>> attributes = new HashSet<>();
		visitIdClassAttributes( attributes::add );

		if ( attributes.isEmpty() ) {
			return null;
		}

		return attributes;
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
		if ( ! hasVersionAttribute() ) {
			return null;
		}

		SingularPersistentAttribute<? super J, ?> version = findVersionAttribute();
		if ( version != null ) {
			checkType( version, javaType );
		}
		return (SingularPersistentAttribute<? super J, Y>) version;
	}

	@Override
	public SingularPersistentAttribute<? super J, ?> findVersionAttribute() {
		if ( versionAttribute != null ) {
			return versionAttribute;
		}

		if ( getSuperType() != null ) {
			return getSuperType().findVersionAttribute();
		}

		return null;
	}

	@Override
	public List<? extends PersistentAttribute<? super J, ?>> findNaturalIdAttributes() {
		if ( naturalIdAttributes != null ) {
			return naturalIdAttributes;
		}

		if ( getSuperType() != null ) {
			return getSuperType().findNaturalIdAttributes();
		}

		return null;
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
	public SingularAttribute<J, ?> getDeclaredVersion() {
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
			AbstractIdentifiableType.this.id = idAttribute;
			managedTypeAccess.addAttribute( idAttribute );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void applyNonAggregatedIdAttributes(
				Set<SingularPersistentAttribute<? super J, ?>> idAttributes,
				EmbeddableDomainType<?> idClassType) {
			if ( AbstractIdentifiableType.this.id != null ) {
				throw new IllegalArgumentException( "`AbstractIdentifiableType#id` already set on call to `#applyNonAggregatedIdAttribute`" );
			}

			if ( nonAggregatedIdAttributes != null ) {
				throw new IllegalStateException( "Non-aggregated id attributes were already set" );
			}

			if ( idAttributes.isEmpty() ) {
				AbstractIdentifiableType.this.nonAggregatedIdAttributes = Collections.EMPTY_SET;
			}
			else {
				for ( SingularPersistentAttribute<? super J, ?> idAttribute : idAttributes ) {
					if ( AbstractIdentifiableType.this == idAttribute.getDeclaringType() ) {
						addAttribute( (PersistentAttribute<J, ?>) idAttribute );
					}
				}

				AbstractIdentifiableType.this.nonAggregatedIdAttributes = idAttributes;
			}
			AbstractIdentifiableType.this.idClassType = idClassType;
		}

		@Override
		public void applyIdClassAttributes(Set<SingularPersistentAttribute<? super J, ?>> idClassAttributes) {
			applyNonAggregatedIdAttributes( idClassAttributes, null );
		}

		@Override
		public void applyVersionAttribute(SingularPersistentAttribute<J, ?> versionAttribute) {
			AbstractIdentifiableType.this.versionAttribute = versionAttribute;
			managedTypeAccess.addAttribute( versionAttribute );
		}

		@Override
		public void applyNaturalIdAttribute(PersistentAttribute<J, ?> naturalIdAttribute) {
			if ( AbstractIdentifiableType.this.naturalIdAttributes == null ) {
				AbstractIdentifiableType.this.naturalIdAttributes = new ArrayList<>();
			}
			AbstractIdentifiableType.this.naturalIdAttributes.add( naturalIdAttribute );
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

		if ( getSuperType() != null && getSuperType().getIdentifierDescriptor() != null ) {
			return getSuperType().getIdentifierDescriptor();
		}
		else if ( id != null ) {
			// simple id or aggregate composite id
			final SimpleDomainType<?> type = id.getType();
			if ( type instanceof BasicDomainType ) {
				return new BasicSqmPathSource<>(
						EntityIdentifierMapping.ID_ROLE_NAME,
						(SqmPathSource) id,
						(BasicDomainType<?>) type,
						type.getExpressibleJavaType(),
						Bindable.BindableType.SINGULAR_ATTRIBUTE,
						id.isGeneric()
				);
			}
			else {
				assert type instanceof EmbeddableDomainType;
				return new EmbeddedSqmPathSource<>(
						EntityIdentifierMapping.ID_ROLE_NAME,
						(SqmPathSource) id,
						(EmbeddableDomainType<?>) type,
						Bindable.BindableType.SINGULAR_ATTRIBUTE,
						id.isGeneric()
				);
			}
		}
		else if ( nonAggregatedIdAttributes != null && ! nonAggregatedIdAttributes.isEmpty() ) {
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
		else {
			if ( isIdMappingRequired() ) {
				throw new UnsupportedMappingException( "Could not build SqmPathSource for entity identifier : " + getTypeName() );
			}
			return null;
		}
	}

	protected boolean isIdMappingRequired() {
		return true;
	}
}
