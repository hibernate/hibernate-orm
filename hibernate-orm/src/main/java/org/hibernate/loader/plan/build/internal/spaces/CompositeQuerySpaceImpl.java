/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.spaces;

import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.persister.entity.PropertyMapping;

/**
 * @author Steve Ebersole
 */
public class CompositeQuerySpaceImpl extends AbstractExpandingSourceQuerySpace implements ExpandingCompositeQuerySpace {
	private final CompositePropertyMapping compositeSubPropertyMapping;

	public CompositeQuerySpaceImpl(
			CompositePropertyMapping compositeSubPropertyMapping,
			String uid,
			ExpandingQuerySpaces querySpaces,
			boolean canJoinsBeRequired) {
		super( uid, Disposition.COMPOSITE, querySpaces, canJoinsBeRequired );
		this.compositeSubPropertyMapping = compositeSubPropertyMapping;
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		return compositeSubPropertyMapping;
	}

	@Override
	public String[] toAliasedColumns(String alias, String propertyName) {
		return compositeSubPropertyMapping.toColumns( alias,propertyName );
	}
}
