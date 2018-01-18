/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Andrea Boriero
 */
public class IdGeneratorResolverSecondPass implements SecondPass {
	private SimpleValue id;
	private XProperty idXProperty;
	private String generatorType;
	private String generatorName;
	private MetadataBuildingContext buildingContext;
	private IdentifierGeneratorDefinition localIdentifierGeneratorDefinition;

	public IdGeneratorResolverSecondPass(
			SimpleValue id,
			XProperty idXProperty,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext) {
		this.id = id;
		this.idXProperty = idXProperty;
		this.generatorType = generatorType;
		this.generatorName = generatorName;
		this.buildingContext = buildingContext;
	}

	public IdGeneratorResolverSecondPass(
			SimpleValue id,
			XProperty idXProperty,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext,
			IdentifierGeneratorDefinition localIdentifierGeneratorDefinition) {
		this(id,idXProperty,generatorType,generatorName,buildingContext);
		this.localIdentifierGeneratorDefinition = localIdentifierGeneratorDefinition;
	}

	@Override
	public void doSecondPass(Map idGeneratorDefinitionMap) throws MappingException {
		BinderHelper.makeIdGenerator( id, idXProperty, generatorType, generatorName, buildingContext, localIdentifierGeneratorDefinition );
	}
}
