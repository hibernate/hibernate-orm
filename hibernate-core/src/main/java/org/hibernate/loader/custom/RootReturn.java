/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;
import org.hibernate.LockMode;
import org.hibernate.loader.EntityAliases;

/**
 * Represents a return which names a "root" entity.
 * <p/>
 * A root entity means it is explicitly a "column" in the result, as opposed to
 * a fetched association.
 *
 * @author Steve Ebersole
 */
public class RootReturn extends NonScalarReturn {
	private final String entityName;
	private final EntityAliases entityAliases;

	public RootReturn(
			String alias,
			String entityName,
			EntityAliases entityAliases,
			LockMode lockMode) {
		super( alias, lockMode );
		this.entityName = entityName;
		this.entityAliases = entityAliases;
	}

	public String getEntityName() {
		return entityName;
	}

	public EntityAliases getEntityAliases() {
		return entityAliases;
	}
}
