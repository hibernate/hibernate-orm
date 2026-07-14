/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTransientImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UnionSubclass;

/**
 * @author Steve Ebersole
 */
public class TransformationHelper {
	public static String determineEntityName(EntityInfo hbmEntity, JaxbHbmHibernateMapping hibernateMapping) {
		if ( StringHelper.isNotEmpty( hbmEntity.getEntityName() ) ) {
			return hbmEntity.getEntityName();
		}

		final String className = hbmEntity.getName();
		assert StringHelper.isNotEmpty( className );
		return StringHelper.qualifyConditionallyIfNot( hibernateMapping.getPackage(), className );
	}

	public static <T> void transfer(Supplier<T> source, Consumer<T> target) {
		final T value = source.get();
		if ( value != null ) {
			target.accept( value );
		}
	}

	static Set<String> discoverAllPropertyNames(Class<?> javaClass, boolean fieldAccess) {
		return discoverUnmappedPropertyNames( javaClass, Set.of(), fieldAccess );
	}

	static Set<String> discoverUnmappedPropertyNames(
			Class<?> javaClass,
			Set<String> mappedPropertyNames,
			boolean fieldAccess) {
		final Set<String> transientNames = new HashSet<>();
		if ( fieldAccess ) {
			for ( var field : javaClass.getDeclaredFields() ) {
				if ( !mappedPropertyNames.contains( field.getName() ) ) {
					transientNames.add( field.getName() );
				}
			}
		}
		else {
			// with property access, the mapped property name from the boot model (e.g. "C1Name")
			// may differ from the decapitalized getter name (e.g. "c1Name") — build a lookup
			// that includes both forms
			final Set<String> effectiveMappedNames = new HashSet<>( mappedPropertyNames );
			for ( String name : mappedPropertyNames ) {
				effectiveMappedNames.add( StringHelper.decapitalize( name ) );
			}
			for ( var method : javaClass.getMethods() ) {
				if ( method.getParameterCount() != 0 ) {
					continue;
				}
				String propertyName = null;
				final String methodName = method.getName();
				if ( methodName.startsWith( "get" ) && methodName.length() > 3 ) {
					propertyName = StringHelper.decapitalize( methodName.substring( 3 ) );
				}
				else if ( methodName.startsWith( "is" ) && methodName.length() > 2
						&& ( method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class ) ) {
					propertyName = StringHelper.decapitalize( methodName.substring( 2 ) );
				}
				if ( propertyName != null
						&& !effectiveMappedNames.contains( propertyName )
						&& !propertyName.equals( "class" ) ) {
					transientNames.add( propertyName );
				}
			}
		}
		return transientNames;
	}

	static void addTransients(Set<String> transientNames, List<JaxbTransientImpl> target) {
		for ( var name : transientNames ) {
			final var transientMapping = new JaxbTransientImpl();
			transientMapping.setName( name );
			target.add( transientMapping );
		}
	}

	public static Table determineEntityTable(PersistentClass persistentClass) {
		if ( persistentClass instanceof RootClass rootClass ) {
			return rootClass.getTable();
		}
		if ( persistentClass instanceof SingleTableSubclass discriminatedSubclass ) {
			return discriminatedSubclass.getRootTable();
		}
		if ( persistentClass instanceof JoinedSubclass joinedSubclass ) {
			return joinedSubclass.getTable();
		}
		if ( persistentClass instanceof UnionSubclass unionSubclass ) {
			return unionSubclass.getTable();
		}
		throw new MappingException( "Unexpected PersistentClass subtype : " + persistentClass );
	}
}
