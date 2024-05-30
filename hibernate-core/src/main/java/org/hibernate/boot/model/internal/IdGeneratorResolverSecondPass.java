/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;

import static org.hibernate.boot.model.internal.GeneratorBinder.makeIdGenerator;

/**
 * @author Andrea Boriero
 */
public class IdGeneratorResolverSecondPass implements SecondPass {
	private final SimpleValue id;
	private final MemberDetails idAttributeMember;
	private final String generatorType;
	private final String generatorName;
	private final MetadataBuildingContext buildingContext;
//	private IdentifierGeneratorDefinition localIdentifierGeneratorDefinition;

	public IdGeneratorResolverSecondPass(
			SimpleValue id,
			MemberDetails idAttributeMember,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext) {
		this.id = id;
		this.idAttributeMember = idAttributeMember;
		this.generatorType = generatorType;
		this.generatorName = generatorName;
		this.buildingContext = buildingContext;
	}

//	public IdGeneratorResolverSecondPass(
//			SimpleValue id,
//			MemberDetails idAttributeMember,
//			String generatorType,
//			String generatorName,
//			MetadataBuildingContext buildingContext,
//			IdentifierGeneratorDefinition localIdentifierGeneratorDefinition) {
//		this( id, idAttributeMember, generatorType, generatorName, buildingContext );
//		this.localIdentifierGeneratorDefinition = localIdentifierGeneratorDefinition;
//	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> idGeneratorDefinitionMap) throws MappingException {
//		makeIdGenerator( id, idAttributeMember, generatorType, generatorName, buildingContext, localIdentifierGeneratorDefinition );
		makeIdGenerator( id, idAttributeMember, generatorType, generatorName, buildingContext, null );
	}

//	public static void makeIdGenerator(
//			SimpleValue id,
//			MemberDetails idAttributeMember,
//			String generatorType,
//			String generatorName,
//			MetadataBuildingContext buildingContext,
//			IdentifierGeneratorDefinition foreignKGeneratorDefinition) {
//		makeIdGenerator( id, idAttributeMember, generatorType, generatorName, buildingContext,
//				foreignKGeneratorDefinition != null
//						? Map.of( foreignKGeneratorDefinition.getName(), foreignKGeneratorDefinition )
//						: null );
//	}
}
