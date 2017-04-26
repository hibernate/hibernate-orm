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
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.spi.Type;

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
		return fromElementAliasMap.computeIfAbsent(
				fromElement,
				e -> generateAliasBase( e.getBinding().getReferencedNavigable() )
		);
	}

	private String generateAliasBase(Navigable domainReference) {
		final String acronym;
		if ( domainReference instanceof EntityValuedExpressableType ) {
			acronym = determineAcronym( (EntityValuedExpressableType) domainReference );
		}
		else if ( domainReference instanceof PersistentAttribute ) {
			acronym = determineAcronym( (PersistentAttribute) domainReference );
		}
		else {
			throw new IllegalArgumentException( "Unexpected Navigable type : " + domainReference );
		}

		Integer acronymCount = acronymCountMap.get( acronym );
		if ( acronymCount == null ) {
			acronymCount = 0;
		}
		acronymCount++;
		acronymCountMap.put( acronym, acronymCount );

		return acronym + acronymCount;
	}

	private String determineAcronym(EntityValuedExpressableType entityRef) {
		return nameAcronymMap.computeIfAbsent(
				entityRef.getEntityName(),
				k -> entityNameToAcronym( entityRef.getEntityName() )
		);
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

	private String determineAcronym(PersistentAttribute attrRef) {
		final String acronymBase;
		final Type attrType = attrRef.getOrmType();
		if ( attrType.isEntityType() && !attrType.isAnyType() ) {
			// use the entity name as the base
			acronymBase = toSimpleEntityName( ( (EntityType) attrType ).getAssociatedEntityName() );
		}
		else {
			acronymBase = attrRef.getAttributeName();
		}

		// see note above, again for now just use the first letter
		return nameAcronymMap.computeIfAbsent(
				acronymBase,
				b -> Character.toString( Character.toLowerCase( b.charAt( 0 ) ) )
		);
	}
}
