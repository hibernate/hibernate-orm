/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;

/**
 * Defines commonality for the JPA {@link IdentifiableType} types.  JPA defines
 * identifiable types as entities or mapped-superclasses.  Basically things to which an
 * identifier can be attached.
 * <p/>
 * NOTE : Currently we only really have support for direct entities in the Hibernate metamodel
 * as the information for them is consumed into the closest actual entity subclass(es) in the
 * internal Hibernate mapping-metamodel.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableType<J>
		extends AbstractManagedType<J>
		implements IdentifiableTypeDescriptor<J>, Serializable {

	private final boolean hasIdentifierProperty;
	private final boolean hasIdClass;
	private SingularPersistentAttribute<J, ?> id;
	private Set<SingularPersistentAttribute<? super J,?>> idClassAttributes;

	private final boolean isVersioned;
	private SingularPersistentAttribute<J, ?> versionAttribute;

	public AbstractIdentifiableType(
			Class<J> javaType,
			String typeName,
			IdentifiableTypeDescriptor<? super J> superType,
			boolean hasIdClass,
			boolean hasIdentifierProperty,
			boolean versioned,
			SessionFactoryImplementor sessionFactory) {
		super( javaType, typeName, superType, sessionFactory );
		this.hasIdClass = hasIdClass;
		this.hasIdentifierProperty = hasIdentifierProperty;
		this.isVersioned = versioned;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected IdentifiableTypeDescriptor.InFlightAccess createInFlightAccess() {
		return new InFlightAccessImpl( super.createInFlightAccess() );
	}

	@Override
	public IdentifiableTypeDescriptor.InFlightAccess<J> getInFlightAccess() {
		return (IdentifiableTypeDescriptor.InFlightAccess<J>) super.getInFlightAccess();
	}

	public boolean hasIdClass() {
		return hasIdClass;
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return !hasIdClass() && hasIdentifierProperty;
	}

	@Override
	@SuppressWarnings("unchecked")
	public IdentifiableTypeDescriptor<? super J> getSuperType() {
		// overridden simply to perform the cast
		return (IdentifiableTypeDescriptor) super.getSuperType();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> SingularPersistentAttribute<? super J, Y> getId(Class<Y> javaType) {
		ensureNoIdClass();
		SingularPersistentAttribute id = locateIdAttribute();
		if ( id != null ) {
			checkType( id, javaType );
		}
		return (SingularPersistentAttribute) id;
	}

	private void ensureNoIdClass() {
		if ( hasIdClass() ) {
			throw new IllegalArgumentException(
					"Illegal call to IdentifiableType#getId for class [" + getTypeName() + "] defined with @IdClass"
			);
		}
	}


	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute locateIdAttribute() {
		if ( id != null ) {
			return id;
		}
		else {
			if ( getSuperType() != null ) {
				SingularPersistentAttribute id = getSuperType().locateIdAttribute();
				if ( id != null ) {
					return id;
				}
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void checkType(SingularPersistentAttribute attribute, Class javaType) {
		if ( ! javaType.isAssignableFrom( attribute.getType().getJavaType() ) ) {
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

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> SingularPersistentAttribute<J, Y> getDeclaredId(Class<Y> javaType) {
		ensureNoIdClass();
		if ( id == null ) {
			throw new IllegalArgumentException( "The id attribute is not declared on this type [" + getTypeName() + "]" );
		}
		checkType( id, javaType );
		return (SingularPersistentAttribute<J, Y>) id;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SimpleTypeDescriptor<?> getIdType() {
		final SingularPersistentAttribute id = locateIdAttribute();
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
	@SuppressWarnings("unchecked")
	public Set<SingularPersistentAttribute<? super J, ?>> getIdClassAttributesSafely() {
		if ( !hasIdClass() ) {
			return null;
		}
		final Set<SingularPersistentAttribute<? super J,?>> attributes = new HashSet<>();
		collectIdClassAttributes( attributes );

		if ( attributes.isEmpty() ) {
			return null;
		}

		return (Set) attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<SingularAttribute<? super J, ?>> getIdClassAttributes() {
		if ( !hasIdClass() ) {
			throw new IllegalArgumentException( "This class [" + getJavaType() + "] does not define an IdClass" );
		}

		final Set<SingularPersistentAttribute<? super J,?>> attributes = new HashSet<>();
		collectIdClassAttributes( attributes );

		if ( attributes.isEmpty() ) {
			throw new IllegalArgumentException( "Unable to locate IdClass attributes [" + getJavaType() + "]" );
		}

		return (Set) attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void collectIdClassAttributes(Set<SingularPersistentAttribute<? super J,?>> attributes) {
		if ( idClassAttributes != null ) {
			attributes.addAll( idClassAttributes );
		}
		else if ( getSuperType() != null ) {
			getSuperType().collectIdClassAttributes( (Set) attributes );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitIdClassAttributes(Consumer<SingularPersistentAttribute<? super J, ?>> attributeConsumer) {
		if ( idClassAttributes != null ) {
			idClassAttributes.forEach( attributeConsumer );
		}
		else if ( getSuperType() != null ) {
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
	@SuppressWarnings({ "unchecked" })
	public <Y> SingularPersistentAttribute<? super J, Y> getVersion(Class<Y> javaType) {
		if ( ! hasVersionAttribute() ) {
			return null;
		}

		SingularPersistentAttribute version = locateVersionAttribute();
		if ( version != null ) {
			checkType( version, javaType );
		}
		return (SingularPersistentAttribute) version;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public SingularPersistentAttribute locateVersionAttribute() {
		if ( versionAttribute != null ) {
			return versionAttribute;
		}

		if ( getSuperType() != null ) {
			return getSuperType().locateVersionAttribute();
		}

		return null;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
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


	/**
	 * For used to retrieve the declared version when populating the static metamodel.
	 *
	 * @return The declared
	 */
	public SingularAttribute<J, ?> getDeclaredVersion() {
		checkDeclaredVersion();
		return versionAttribute;
	}

	private class InFlightAccessImpl implements IdentifiableTypeDescriptor.InFlightAccess<J> {
		private final AbstractManagedType.InFlightAccess managedTypeAccess;

		private InFlightAccessImpl(ManagedTypeDescriptor.InFlightAccess managedTypeAccess) {
			this.managedTypeAccess = managedTypeAccess;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void applyIdAttribute(SingularPersistentAttribute<J, ?> idAttribute) {
			AbstractIdentifiableType.this.id = idAttribute;
			managedTypeAccess.addAttribute( idAttribute );
		}

		@Override
		public void applyIdClassAttributes(Set<SingularPersistentAttribute<? super J,?>> idClassAttributes) {
			for ( SingularAttribute<? super J,?> idClassAttribute : idClassAttributes ) {
				if ( AbstractIdentifiableType.this == idClassAttribute.getDeclaringType() ) {
					@SuppressWarnings({ "unchecked" })
					SingularPersistentAttribute<J,?> declaredAttribute = (SingularPersistentAttribute) idClassAttribute;
					addAttribute( declaredAttribute );
				}
			}
			AbstractIdentifiableType.this.idClassAttributes = idClassAttributes;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void applyVersionAttribute(SingularPersistentAttribute<J, ?> versionAttribute) {
			AbstractIdentifiableType.this.versionAttribute = versionAttribute;
			managedTypeAccess.addAttribute( versionAttribute );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void addAttribute(PersistentAttributeDescriptor attribute) {
			managedTypeAccess.addAttribute( attribute );
		}

		@Override
		public void finishUp() {
			managedTypeAccess.finishUp();
		}
	}
}
