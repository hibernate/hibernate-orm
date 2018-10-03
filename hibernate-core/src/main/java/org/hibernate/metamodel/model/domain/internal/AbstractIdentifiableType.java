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
import org.hibernate.metamodel.model.domain.spi.AttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.SingularAttributeImplementor;

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
		implements IdentifiableTypeImplementor<J>, Serializable {

	private final boolean hasIdentifierProperty;
	private final boolean hasIdClass;
	private SingularAttributeImplementor<J, ?> id;
	private Set<SingularAttributeImplementor<? super J,?>> idClassAttributes;

	private final boolean isVersioned;
	private SingularAttributeImplementor<J, ?> versionAttribute;

	public AbstractIdentifiableType(
			Class<J> javaType,
			String typeName,
			IdentifiableTypeImplementor<? super J> superType,
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
	protected IdentifiableTypeImplementor.InFlightAccess createInFlightAccess() {
		return new InFlightAccessImpl( super.createInFlightAccess() );
	}

	@Override
	public IdentifiableTypeImplementor.InFlightAccess<J> getInFlightAccess() {
		return (IdentifiableTypeImplementor.InFlightAccess<J>) super.getInFlightAccess();
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
	public IdentifiableTypeImplementor<? super J> getSuperType() {
		// overridden simply to perform the cast
		return (IdentifiableTypeImplementor) super.getSuperType();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> SingularAttributeImplementor<? super J, Y> getId(Class<Y> javaType) {
		ensureNoIdClass();
		SingularAttributeImplementor id = locateIdAttribute();
		if ( id != null ) {
			checkType( id, javaType );
		}
		return (SingularAttributeImplementor) id;
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
	public SingularAttributeImplementor locateIdAttribute() {
		if ( id != null ) {
			return id;
		}
		else {
			if ( getSuperType() != null ) {
				SingularAttributeImplementor id = getSuperType().locateIdAttribute();
				if ( id != null ) {
					return id;
				}
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void checkType(SingularAttributeImplementor attribute, Class javaType) {
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
	public <Y> SingularAttributeImplementor<J, Y> getDeclaredId(Class<Y> javaType) {
		ensureNoIdClass();
		if ( id == null ) {
			throw new IllegalArgumentException( "The id attribute is not declared on this type [" + getTypeName() + "]" );
		}
		checkType( id, javaType );
		return (SingularAttributeImplementor<J, Y>) id;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SimpleTypeImplementor<?> getIdType() {
		final SingularAttributeImplementor id = locateIdAttribute();
		if ( id != null ) {
			return id.getType();
		}

		Set<SingularAttributeImplementor<? super J, ?>> idClassAttributes = getIdClassAttributesSafely();
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
	public Set<SingularAttributeImplementor<? super J, ?>> getIdClassAttributesSafely() {
		if ( !hasIdClass() ) {
			return null;
		}
		final Set<SingularAttributeImplementor<? super J,?>> attributes = new HashSet<>();
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

		final Set<SingularAttributeImplementor<? super J,?>> attributes = new HashSet<>();
		collectIdClassAttributes( attributes );

		if ( attributes.isEmpty() ) {
			throw new IllegalArgumentException( "Unable to locate IdClass attributes [" + getJavaType() + "]" );
		}

		return (Set) attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void collectIdClassAttributes(Set<SingularAttributeImplementor<? super J,?>> attributes) {
		if ( idClassAttributes != null ) {
			attributes.addAll( idClassAttributes );
		}
		else if ( getSuperType() != null ) {
			getSuperType().collectIdClassAttributes( (Set) attributes );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitIdClassAttributes(Consumer<SingularAttributeImplementor<? super J, ?>> attributeConsumer) {
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
	public <Y> SingularAttributeImplementor<? super J, Y> getVersion(Class<Y> javaType) {
		if ( ! hasVersionAttribute() ) {
			return null;
		}

		SingularAttributeImplementor version = locateVersionAttribute();
		if ( version != null ) {
			checkType( version, javaType );
		}
		return (SingularAttributeImplementor) version;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public SingularAttributeImplementor locateVersionAttribute() {
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
	public <Y> SingularAttributeImplementor<J, Y> getDeclaredVersion(Class<Y> javaType) {
		checkDeclaredVersion();
		checkType( versionAttribute, javaType );
		return ( SingularAttributeImplementor<J, Y> ) versionAttribute;
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

	private class InFlightAccessImpl implements IdentifiableTypeImplementor.InFlightAccess<J> {
		private final AbstractManagedType.InFlightAccess managedTypeAccess;

		private InFlightAccessImpl(ManagedTypeImplementor.InFlightAccess managedTypeAccess) {
			this.managedTypeAccess = managedTypeAccess;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void applyIdAttribute(SingularAttributeImplementor<J, ?> idAttribute) {
			AbstractIdentifiableType.this.id = idAttribute;
			managedTypeAccess.addAttribute( idAttribute );
		}

		@Override
		public void applyIdClassAttributes(Set<SingularAttributeImplementor<? super J,?>> idClassAttributes) {
			for ( SingularAttribute<? super J,?> idClassAttribute : idClassAttributes ) {
				if ( AbstractIdentifiableType.this == idClassAttribute.getDeclaringType() ) {
					@SuppressWarnings({ "unchecked" })
					SingularAttributeImplementor<J,?> declaredAttribute = (SingularAttributeImplementor) idClassAttribute;
					addAttribute( declaredAttribute );
				}
			}
			AbstractIdentifiableType.this.idClassAttributes = idClassAttributes;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void applyVersionAttribute(SingularAttributeImplementor<J, ?> versionAttribute) {
			AbstractIdentifiableType.this.versionAttribute = versionAttribute;
			managedTypeAccess.addAttribute( versionAttribute );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void addAttribute(AttributeImplementor attribute) {
			managedTypeAccess.addAttribute( attribute );
		}

		@Override
		public void finishUp() {
			managedTypeAccess.finishUp();
		}
	}
}
