/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.ArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;

/**
 * A type that maps between {@link java.sql.Types#ARRAY ARRAY} and {@code T[]}
 *
 * @author Jordan Gigov
 * @author Christian Beikov
 */
public class BasicArrayType<T>
		extends AbstractSingleColumnStandardBasicType<T[]>
		implements SqlTypeDescriptorIndicatorCapable<T[]>, BasicContainerType<T> {

	private final BasicType<T> baseDescriptor;
	private final String name;
	private final String[] registrationKeys;

	public BasicArrayType(BasicType<T> baseDescriptor, JavaTypeDescriptor<T[]> arrayTypeDescriptor) {
		super( ArrayTypeDescriptor.INSTANCE, arrayTypeDescriptor );
		this.baseDescriptor = baseDescriptor;
		this.name = baseDescriptor.getName() + "[]";
		final String[] registrationKeys = baseDescriptor.getRegistrationKeys();
		this.registrationKeys = buildTypeRegistrations(
				registrationKeys,
				baseDescriptor instanceof BasicContainerType<?>
		);
	}

	@Override
	public BasicType<T> getElementType() {
		return baseDescriptor;
	}

	/**
	 * Builds the array registration keys, based on the original type's keys.
	 *
	 * @param baseKeys Array of keys used by the base type.
	 */
	private String[] buildTypeRegistrations(String[] baseKeys, boolean baseIsContainer) {
		List<String> keys = new ArrayList<>( baseKeys.length << 1 );
		for ( String bk : baseKeys ) {
			if ( bk != null ) {
				boolean addSqlType = true;
				try {
					Class<?> c;
					switch ( bk ) {
						case "boolean":
							c = boolean.class;
							break;
						case "byte":
							c = byte.class;
							break;
						case "char":
							c = char.class;
							break;
						case "double":
							c = double.class;
							break;
						case "float":
							c = float.class;
							break;
						case "int":
							c = int.class;
							break;
						case "long":
							c = long.class;
							break;
						case "short":
							c = short.class;
							break;
						default:
							// load to make sure it exists
							c = Class.forName( bk );
							addSqlType = false;
					}
					keys.add( c.getTypeName() + "[]" );
				}
				catch (ClassNotFoundException ex) {
				}
				if ( addSqlType ) {
					// Not all type names given are Java classes, so assume the others are Database types
					if ( baseIsContainer ) {
						// type is just "basetype ARRAY", never "basetype ARRAY ARRAY ARRAY"
						keys.add( bk );
					}
					else {
						// standard SQL
						keys.add( bk + " ARRAY" );
						// also possible
						keys.add( bk + " array" );
					}
				}
			}
		}
		return keys.toArray( new String[keys.size()] );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String[] getRegistrationKeys() {
		return registrationKeys.clone();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(SqlTypeDescriptorIndicators indicators) {
		// TODO: maybe fallback to some encoding by default if the DB doesn't support arrays natively?
		//noinspection unchecked
		return (BasicType<X>) this;
	}
}
