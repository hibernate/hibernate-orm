/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
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
