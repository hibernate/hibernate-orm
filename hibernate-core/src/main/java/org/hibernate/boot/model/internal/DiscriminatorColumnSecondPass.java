/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.persister.entity.DiscriminatorHelper;


public class DiscriminatorColumnSecondPass implements SecondPass {
	private final String rootEntityName;
	private final Dialect dialect;

	public DiscriminatorColumnSecondPass(String rootEntityName, Dialect dialect) {
		this.rootEntityName = rootEntityName;
		this.dialect = dialect;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final PersistentClass rootClass = persistentClasses.get( rootEntityName );
		if ( hasNullDiscriminatorValue( rootClass ) ) {
			for ( Selectable selectable: rootClass.getDiscriminator().getSelectables() ) {
				if ( selectable instanceof Column column ) {
					column.setNullable( true );
				}
			}
		}
		if ( !hasNotNullDiscriminatorValue( rootClass ) // a "not null" discriminator is a catch-all
				&& !rootClass.getDiscriminator().hasFormula() // can't add check constraints to formulas
				&& !rootClass.isForceDiscriminator() ) { // the usecase for "forced" discriminators is that there are some rogue values
			final Column column = rootClass.getDiscriminator().getColumns().get( 0 );
			column.addCheckConstraint( new CheckConstraint( checkConstraint( rootClass, column ) ) );
		}
	}

	private boolean hasNullDiscriminatorValue(PersistentClass rootClass) {
		if ( rootClass.isDiscriminatorValueNull() ) {
			return true;
		}
		for ( Subclass subclass : rootClass.getSubclasses() ) {
			if ( subclass.isDiscriminatorValueNull() ) {
				return true;
			}
		}
		return false;
	}

	private boolean hasNotNullDiscriminatorValue(PersistentClass rootClass) {
		if ( rootClass.isDiscriminatorValueNotNull() ) {
			return true;
		}
		for ( Subclass subclass : rootClass.getSubclasses() ) {
			if ( subclass.isDiscriminatorValueNotNull() ) {
				return true;
			}
		}
		return false;
	}

	private String checkConstraint(PersistentClass rootClass, Column column) {
		return dialect.getCheckCondition(
				column.getQuotedName( dialect ),
				discriminatorValues( rootClass ),
				column.getType().getJdbcType()
		);
	}

	private static List<String> discriminatorValues(PersistentClass rootClass) {
		final List<String> values = new ArrayList<>();
		if ( !rootClass.isAbstract()
				&& !rootClass.isDiscriminatorValueNull()
				&& !rootClass.isDiscriminatorValueNotNull() ) {
			values.add( DiscriminatorHelper.getDiscriminatorValue( rootClass ).toString() );
		}
		for ( Subclass subclass : rootClass.getSubclasses() ) {
			if ( !subclass.isAbstract()
					&& !subclass.isDiscriminatorValueNull()
					&& !subclass.isDiscriminatorValueNotNull() ) {
				values.add( DiscriminatorHelper.getDiscriminatorValue( subclass ).toString() );
			}
		}
		return values;
	}
}
