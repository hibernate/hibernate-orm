/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.FetchMode;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.type.Type;

/**
 * @deprecated Replaced by {@link org.hibernate.metamodel.mapping.AttributeMapping}
 */
@Deprecated(forRemoval = true)
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
	 * @param nullable Is this property a nullable value?
	 * @param cascadeStyle The cascade style for this property's value.
	 * @param fetchMode Any fetch mode defined for this property
	 */
	public StandardProperty(
			String name,
			Type type,
			boolean lazy,
			boolean insertable,
			boolean updateable,
			boolean nullable,
			boolean checkable,
			boolean versionable,
			CascadeStyle cascadeStyle,
			OnDeleteAction onDeleteAction,
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
						.setNullable( nullable )
						.setDirtyCheckable( checkable )
						.setVersionable( versionable )
						.setCascadeStyle( cascadeStyle )
						.setOnDeleteAction( onDeleteAction )
						.setFetchMode( fetchMode )
						.createInformation()
		);
	}
}
