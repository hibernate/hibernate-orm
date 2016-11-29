/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

/**
 * @author Steve Ebersole
 */
public class EntityDescriptor
		extends IdentifiableTypeDescriptor
		implements JavaTypeDescriptorEntityImplementor {

	protected EntityDescriptor(String typeName) {
		this( typeName, null );
	}

	public EntityDescriptor(String typeName, EntityHierarchy entityHierarchy) {
		this(
				typeName,
				entityHierarchy,
				null,
				null
		);
	}

	public EntityDescriptor(
			String typeName,
			EntityHierarchy entityHierarchy,
			Class javaType,
			ManagedTypeDescriptor superTypeDescriptor) {
		super( typeName, entityHierarchy, javaType, superTypeDescriptor );
	}

	@Override
	public String getName() {
		return getTypeName();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	@Override
	public Class getBindableJavaType() {
		return getJavaTypeClass();
	}
}
