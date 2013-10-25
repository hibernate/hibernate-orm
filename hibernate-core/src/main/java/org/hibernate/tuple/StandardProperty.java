/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tuple;

import org.hibernate.FetchMode;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.type.Type;

/**
 * Represents a non-identifier property within the Hibernate runtime-metamodel.
 *
 * @author Steve Ebersole
 */
@Deprecated
public class StandardProperty extends AbstractNonIdentifierAttribute implements NonIdentifierAttribute {

	/**
	 * Constructs NonIdentifierProperty instances.
	 *
	 * @param name The name by which the property can be referenced within
	 * its owner.
	 * @param type The Hibernate Type of this property.
	 * @param lazy Should this property be handled lazily?
	 * @param insertable Is this property an insertable value?
	 * @param updateable Is this property an updateable value?
	 * @param valueGenerationStrategy How (if) values for this attribute are generated
	 * @param nullable Is this property a nullable value?
	 * @param checkable Is this property a checkable value?
	 * @param versionable Is this property a versionable value?
	 * @param cascadeStyle The cascade style for this property's value.
	 * @param fetchMode Any fetch mode defined for this property
	 */
	public StandardProperty(
			String name,
			Type type,
			boolean lazy,
			boolean insertable,
			boolean updateable,
			ValueGeneration valueGenerationStrategy,
			boolean nullable,
			boolean checkable,
			boolean versionable,
			CascadeStyle cascadeStyle,
			FetchMode fetchMode) {
		super(
				null,
				null,
				-1,
				name,
				type,
				new BaselineAttributeInformation.Builder()
						.setLazy( lazy )
						.setInsertable( insertable )
						.setUpdateable( updateable )
						.setValueGenerationStrategy( valueGenerationStrategy )
						.setNullable( nullable )
						.setDirtyCheckable( checkable )
						.setVersionable( versionable )
						.setCascadeStyle( cascadeStyle )
						.setFetchMode( fetchMode )
						.createInformation()
		);
	}
}
