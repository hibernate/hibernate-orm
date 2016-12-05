/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

import org.hibernate.EntityMode;

/**
 * @author Steve Ebersole
 */
public class RootEntityDescriptor extends EntityDescriptor {
	private final EntityHierarchyImpl entityHierarchy;

	public RootEntityDescriptor(
			String typeName,
			EntityHierarchy.InheritanceStyle inheritanceStyle,
			EntityMode entityMode) {
		super( typeName, null, null, null );
		this.entityHierarchy = new EntityHierarchyImpl( this, inheritanceStyle, entityMode );
	}

	@Override
	public EntityHierarchy getEntityHierarchy() {
		return entityHierarchy;
	}
}
