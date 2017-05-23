/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.FetchMode;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.type.Type;

/**
 * Represents a non-identifier property within the Hibernate runtime-metamodel.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use one of the {@link org.hibernate.tuple.Attribute}-based impls instead.
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
