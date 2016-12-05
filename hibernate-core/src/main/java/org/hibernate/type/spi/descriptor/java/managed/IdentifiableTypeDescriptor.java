/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

import java.util.Set;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.cfg.NotYetImplementedException;

/**
 * @author Steve Ebersole
 */
public abstract class IdentifiableTypeDescriptor
		extends ManagedTypeDescriptor
		implements JavaTypeDescriptorIdentifiableImplementor {

	private final EntityHierarchy entityHierarchy;

	public IdentifiableTypeDescriptor(String typeName, EntityHierarchy entityHierarchy) {
		this( typeName, entityHierarchy, null, null );
	}

	public IdentifiableTypeDescriptor(
			String typeName,
			EntityHierarchy entityHierarchy,
			Class javaType,
			ManagedTypeDescriptor superTypeDescriptor) {
		super( typeName, javaType, superTypeDescriptor );
		this.entityHierarchy = entityHierarchy;
	}

	@Override
	public IdentifiableTypeDescriptor getSupertype() {
		return (IdentifiableTypeDescriptor) super.getSupertype();
	}

	@Override
	public EntityHierarchy getEntityHierarchy() {
		return entityHierarchy;
	}

	@Override
	public SingularAttribute getId(Class type) {
		throw new NotYetImplementedException( );
	}

	@Override
	public SingularAttribute getDeclaredId(Class type) {
		throw new NotYetImplementedException( );
	}

	@Override
	public SingularAttribute getVersion(Class type) {
		throw new NotYetImplementedException( );
	}

	@Override
	public SingularAttribute getDeclaredVersion(Class type) {
		throw new NotYetImplementedException( );
	}

	@Override
	public boolean hasSingleIdAttribute() {
		throw new NotYetImplementedException( );
	}

	@Override
	public boolean hasVersionAttribute() {
		throw new NotYetImplementedException( );
	}

	@Override
	public Set<SingularAttribute> getIdClassAttributes() {
		throw new NotYetImplementedException( );
	}

	@Override
	public Type<?> getIdType() {
		throw new NotYetImplementedException( );
	}
}
