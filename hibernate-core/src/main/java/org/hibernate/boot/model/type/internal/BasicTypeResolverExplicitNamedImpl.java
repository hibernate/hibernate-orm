/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.spi.BasicType;

/**
 * @author Chris Cranford
 */
public class BasicTypeResolverExplicitNamedImpl implements BasicTypeResolver {
	private final MetadataBuildingContext buildingContext;
	private final String name;

	public BasicTypeResolverExplicitNamedImpl(MetadataBuildingContext buildingContext, String typeName) {
		this.buildingContext = buildingContext;
		this.name = typeName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> BasicType<T> resolveBasicType(ResolutionContext context) {
		return buildingContext.getBootstrapContext().getTypeConfiguration()
				.getBasicTypeRegistry()
				.getBasicType( name );
	}
}
