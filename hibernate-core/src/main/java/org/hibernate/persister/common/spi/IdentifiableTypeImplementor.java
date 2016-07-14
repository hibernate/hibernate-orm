/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.spi;

import org.hibernate.persister.common.internal.DatabaseModel;
import org.hibernate.persister.common.internal.DomainMetamodelImpl;
import org.hibernate.sqm.domain.IdentifiableType;

/**
 * @author Steve Ebersole
 */
public interface IdentifiableTypeImplementor extends IdentifiableType {
	void finishInitialization(
			IdentifiableTypeImplementor superType,
			Object typeSource,
			DatabaseModel databaseModel,
			DomainMetamodelImpl domainMetamodel);

	@Override
	IdentifiableTypeImplementor getSuperType();
}
