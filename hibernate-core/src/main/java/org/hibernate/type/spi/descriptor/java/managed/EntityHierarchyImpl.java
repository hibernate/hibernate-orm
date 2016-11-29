/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

import org.hibernate.EntityMode;
import org.hibernate.cfg.NotYetImplementedException;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private final EntityDescriptor rootEntityDescriptor;
	private final InheritanceStyle inheritanceStyle;
	private final EntityMode entityMode;

	public EntityHierarchyImpl(
			EntityDescriptor rootEntityDescriptor,
			InheritanceStyle inheritanceStyle,
			EntityMode entityMode) {
		this.rootEntityDescriptor = rootEntityDescriptor;
		this.inheritanceStyle = inheritanceStyle;
		this.entityMode = entityMode;
	}

	@Override
	public InheritanceStyle getInheritanceStyle() {
		return inheritanceStyle;
	}

	@Override
	public JavaTypeDescriptorEntityImplementor getRootEntityDescriptor() {
		return rootEntityDescriptor;
	}

	@Override
	public EntityMode getEntityMode() {
		return entityMode;
	}

	@Override
	public IdentifierDescriptor getIdentifierDescriptor() {
		throw new NotYetImplementedException( "IdentifierDescriptor support as part of EntityHierarchyImpl not yet implemented" );
	}
}
