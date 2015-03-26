/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.internal.util.compare.EqualsHelper;

/**
 * Models the information pertaining to a custom type definition supplied by the user.  Used
 * to delay instantiation of the actual {@link org.hibernate.type.Type} instance.
 *
 * Generally speaking this information would come from annotations
 * ({@link org.hibernate.annotations.TypeDef}) or XML mappings.  An alternative form of
 * supplying custom types is programatically via one of:<ul>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.type.BasicType)}</li>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.usertype.UserType, String[])}</li>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyTypes(TypeContributor)}</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author John Verhaeg
 */
public class TypeDefinition implements Serializable {
	private final String name;
    private final Class typeImplementorClass;
	private final String[] registrationKeys;
    private final Map<String, String> parameters;

	public TypeDefinition(
			String name,
			Class typeImplementorClass,
			String[] registrationKeys,
			Map<String, String> parameters) {
		this.name = name;
		this.typeImplementorClass = typeImplementorClass;
		this.registrationKeys= registrationKeys;
		this.parameters = parameters == null
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap( parameters );
	}

	public TypeDefinition(
			String name,
			Class typeImplementorClass,
			String[] registrationKeys,
			Properties parameters) {
		this.name = name;
		this.typeImplementorClass = typeImplementorClass;
		this.registrationKeys= registrationKeys;
		this.parameters = parameters == null
				? Collections.<String, String>emptyMap()
				: extractStrings( parameters );
	}

	private Map<String, String> extractStrings(Properties properties) {
		final Map<String, String> parameters = new HashMap<String, String>();

		for ( Map.Entry entry : properties.entrySet() ) {
			if ( String.class.isInstance( entry.getKey() )
					&& String.class.isInstance( entry.getValue() ) ) {
				parameters.put(
						(String) entry.getKey(),
						(String) entry.getValue()
				);
			}
		}

		return parameters;
	}

	public String getName() {
		return name;
	}

	public Class getTypeImplementorClass() {
		return typeImplementorClass;
	}

	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	public Map<String, String> getParameters() {
        return parameters;
    }

	public Properties getParametersAsProperties() {
		Properties properties = new Properties();
		properties.putAll( parameters );
		return properties;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof TypeDefinition ) ) {
			return false;
		}

		final TypeDefinition that = (TypeDefinition) o;
		return EqualsHelper.equals( this.name, that.name )
				&& EqualsHelper.equals( this.typeImplementorClass, that.typeImplementorClass )
				&& Arrays.equals( this.registrationKeys, that.registrationKeys )
				&& EqualsHelper.equals( this.parameters, that.parameters );
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( typeImplementorClass != null ? typeImplementorClass.hashCode() : 0 );
		result = 31 * result + ( registrationKeys != null ? Arrays.hashCode( registrationKeys ) : 0 );
		result = 31 * result + ( parameters != null ? parameters.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "TypeDefinition{" +
				"name='" + name + '\'' +
				", typeImplementorClass=" + typeImplementorClass +
				", registrationKeys=" + Arrays.toString( registrationKeys ) +
				", parameters=" + parameters +
				'}';
	}
}
