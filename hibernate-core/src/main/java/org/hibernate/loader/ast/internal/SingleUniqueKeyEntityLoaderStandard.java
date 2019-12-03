/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;

/**
 * @author Steve Ebersole
 */
public class SingleUniqueKeyEntityLoaderStandard implements SingleUniqueKeyEntityLoader {
	private final EntityMappingType entityDescriptor;
	private final SingularAttributeMapping uniqueKeyAttribute;

	public SingleUniqueKeyEntityLoaderStandard(
			EntityMappingType entityDescriptor,
			SingularAttributeMapping uniqueKeyAttribute) {
		this.entityDescriptor = entityDescriptor;
		this.uniqueKeyAttribute = uniqueKeyAttribute;
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Override
	public Object load(
			Object ukValue,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
