/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.sqm.domain.AttributeReference;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.sqm.query.from.SqmFrom;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class SqlAliasBaseManager {
	// an overall dictionary; used to ensure that a given FromElement instance always
	// resolves to the same alias-base (its called base because a FromElement can encompass
	// multiple physical tables).
	private Map<SqmFrom,String> fromElementAliasMap = new HashMap<>();

	// work dictionary used to map a FromElement type name to its acronym
	private Map<String,String> nameAcronymMap = new HashMap<>();
	// wok dictionary used to map an acronym to the number of times it has been used.
	private Map<String,Integer> acronymCountMap = new HashMap<>();

	public String getSqlAliasBase(SqmFrom fromElement) {
		String aliasBase = fromElementAliasMap.get( fromElement );
		if ( aliasBase == null ) {
			aliasBase = generateAliasBase( fromElement.getDomainReferenceBinding().getBoundDomainReference() );
			fromElementAliasMap.put( fromElement, aliasBase );
		}
		return aliasBase;
	}

	private String generateAliasBase(DomainReference domainReference) {
		final String acronym;
		if ( domainReference instanceof EntityReference ) {
			acronym = determineAcronym( (EntityReference) domainReference );
		}
		else if ( domainReference instanceof AttributeReference ) {
			acronym = determineAcronym( (AttributeReference) domainReference );
		}
		else {
			throw new IllegalArgumentException( "Unexpected DomainReference type : " + domainReference );
		}

		Integer acronymCount = acronymCountMap.get( acronym );
		if ( acronymCount == null ) {
			acronymCount = 0;
		}
		acronymCount++;
		acronymCountMap.put( acronym, acronymCount );

		return acronym + acronymCount;
	}

	private String determineAcronym(EntityReference entityRef) {
		String acronym = nameAcronymMap.get( entityRef.getEntityName() );
		if ( acronym == null ) {
			acronym = entityNameToAcronym( entityRef.getEntityName() );
			nameAcronymMap.put( entityRef.getEntityName(), acronym );
		}
		return acronym;
	}

	private String entityNameToAcronym(String entityName) {
		final String simpleName = toSimpleEntityName( entityName );

		// ideally I'd like to build the alias base from acronym form of the name.  E.g.
		// 'TransportationMethod` becomes 'tm', 'ShippingDestination` becomes 'sd', etc

		// for now, just use the first letter
		return Character.toString( Character.toLowerCase( simpleName.charAt( 0 ) ) );
	}

	private String toSimpleEntityName(String entityName) {
		String simpleName = StringHelper.unqualify( entityName );
		if ( simpleName.contains( "$" ) ) {
			// inner class
			simpleName = simpleName.substring( simpleName.lastIndexOf( '$' ) + 1 );
		}
		if ( StringHelper.isEmpty( simpleName ) ) {
			throw new AssertionFailure( "Could not determine simple name as base for alias [" + entityName + "]" );
		}
		return simpleName;
	}

	private String determineAcronym(AttributeReference attrRef) {
		final String acronymBase;
		final Type attrType = ( (Attribute) attrRef ).getOrmType();
		if ( attrType.isEntityType() && !attrType.isAnyType() ) {
			// use the entity name as the base
			acronymBase = toSimpleEntityName( ( (EntityType) attrType ).getAssociatedEntityName() );
		}
		else {
			acronymBase = attrRef.getAttributeName();
		}

		String acronym = nameAcronymMap.get( acronymBase );
		if ( acronym == null ) {
			// see note above, again for now just use the first letter
			acronym = Character.toString( Character.toLowerCase( acronymBase.charAt( 0 ) ) );
			nameAcronymMap.put( acronymBase, acronym );
		}
		return acronym;
	}
}
