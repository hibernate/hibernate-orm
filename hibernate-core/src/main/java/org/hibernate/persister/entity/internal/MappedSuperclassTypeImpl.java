/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.internal;

import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.persister.common.internal.DatabaseModel;
import org.hibernate.persister.common.internal.DomainMetamodelImpl;
import org.hibernate.persister.common.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.common.spi.MappedSuperclassTypeImplementor;
import org.hibernate.sqm.domain.Attribute;
import org.hibernate.sqm.domain.IdentifierDescriptor;
import org.hibernate.sqm.domain.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeImpl implements MappedSuperclassTypeImplementor {
	private String typeName;
	private IdentifiableTypeImplementor superType;

	@Override
	public void finishInitialization(
			IdentifiableTypeImplementor superType,
			Object typeSource,
			DatabaseModel databaseModel,
			DomainMetamodelImpl domainMetamodel) {
		this.superType = superType;
		this.typeName = ( (MappedSuperclass) typeSource ).getMappedClass().getName();
	}

	@Override
	public IdentifiableTypeImplementor getSuperType() {
		return superType;
	}

	@Override
	public Attribute findAttribute(String name) {
		return null;
	}

	@Override
	public Attribute findDeclaredAttribute(String name) {
		return null;
	}

	@Override
	public IdentifierDescriptor getIdentifierDescriptor() {
		return null;
	}

	@Override
	public SingularAttribute getVersionAttribute() {
		return null;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}
}
