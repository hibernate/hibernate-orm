/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.internal;

import org.hibernate.persister.common.spi.SingularAttributeImplementor;
import org.hibernate.persister.entity.spi.EntityVersion;

/**
 * @author Steve Ebersole
 */
public class EntityVersionImpl implements EntityVersion {
	private final SingularAttributeImplementor versionAttribute;
	private final String unsavedValue;

	public EntityVersionImpl(SingularAttributeImplementor versionAttribute, String unsavedValue) {
		this.versionAttribute = versionAttribute;
		this.unsavedValue = unsavedValue;
	}

	@Override
	public SingularAttributeImplementor getVersionAttribute() {
		return versionAttribute;
	}

	@Override
	public String getUnsavedValue() {
		return unsavedValue;
	}
}
