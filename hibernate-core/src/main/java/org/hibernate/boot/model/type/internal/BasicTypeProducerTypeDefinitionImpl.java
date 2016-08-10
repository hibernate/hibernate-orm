/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.type.spi.AbstractBasicTypeProducer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BasicTypeProducerTypeDefinitionImpl extends AbstractBasicTypeProducer {
	private final String name;
	private final TypeDefinition typeDefinition;

	public BasicTypeProducerTypeDefinitionImpl(String name, TypeDefinition typeDefinition, TypeConfiguration typeConfiguration) {
		super( typeConfiguration );

		// we use assertions here because BasicTypeProducerRegistryImpl already performs an if check.
		// The assertions here are nice still for unit tests

		assert StringHelper.isEmpty( name ) : "Registration name is required";
		assert typeDefinition.getTypeImplementorClass() == null : "Name of BasicType implementation class is required";

		this.name = name;
		this.typeDefinition = typeDefinition;
	}

	@Override
	protected Map<String,?> collectTypeParameters() {
		HashMap<String,Object> params = null;

		if ( CollectionHelper.isNotEmpty( typeDefinition.getParameters() ) ) {
			params = new HashMap<>();
			params.putAll( typeDefinition.getParameters() );
		}

		if ( getBasicTypeSiteContext() != null && CollectionHelper.isNotEmpty( getBasicTypeSiteContext().getLocalTypeParameters() ) ) {
			if ( params == null ) {
				params = new HashMap<>();
			}
			params.putAll( getBasicTypeSiteContext().getLocalTypeParameters() );
		}

		return params;
	}

	@Override
	public String getName() {
		return name;
	}
}
