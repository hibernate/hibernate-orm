/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.transform;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.transform.ResultTransformer;

/**
 * Based on class : {@link AliasToBeanResultTransformer}
 *
 * Result transformer that allows to transform a result to
 * a user specified class which will be populated via setter
 * methods or fields matching the alias names (ignore case) or columns names.
 * <p/>
 * <pre>
 * List resultWithAliasedBean = s.createCriteria(Enrolment.class)
 *   		.createAlias("student", "st")
 * 			.createAlias("course", "co")
 * 			.setProjection( Projections.projectionList()
 * 					.add( Projections.property("co.description"), "courseDescription" )
 * 			)
 * 			.setResultTransformer( new AliasToBeanIgnoreCaseResultTransformer(StudentDTO.class) )
 * 			.list();
 * <p/>
 *  StudentDTO dto = (StudentDTO)resultWithAliasedBean.get(0);
 * 	</pre>
 *
 * @author Javier Ledo Vázquez (javier.ledo@tecnocom.es)
 */
public class AliasToBeanIgnoreCaseResultTransformer implements ResultTransformer, Serializable {

	// IMPL NOTE : due to the delayed population of setters (setters cached
	// 		for performance), we really cannot properly define equality for
	// 		this transformer

	private final Class resultClass;
	private boolean isInitialized;
	private String[] aliases;
	private Setter[] setters;

	public AliasToBeanIgnoreCaseResultTransformer(Class resultClass) {
		if ( resultClass == null ) {
			throw new IllegalArgumentException( "resultClass cannot be null" );
		}
		isInitialized = false;
		this.resultClass = resultClass;
	}

	public Object transformTuple(Object[] tuple, String[] aliases) {
		Object result;

		try {
			if ( ! isInitialized ) {
				initialize( aliases );
			}
			else {
				check( aliases );
			}
			
			result = resultClass.newInstance();

			for ( int i = 0; i < aliases.length; i++ ) {
				if ( setters[i] != null ) {
					setters[i].set( result, tuple[i], null );
				}
			}
		}
		catch ( InstantiationException e ) {
			throw new HibernateException( "Could not instantiate resultclass: " + resultClass.getName() );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "Could not instantiate resultclass: " + resultClass.getName() );
		}

		return result;
	}

	private void initialize(String[] aliases) {
		PropertyAccessor propertyAccessor = new ChainedPropertyAccessor(
				new PropertyAccessor[] {
						PropertyAccessorFactory.getPropertyAccessor( resultClass, null ),
						PropertyAccessorFactory.getPropertyAccessor( "field" )
				}
		);
		this.aliases = new String[ aliases.length ];
		setters = new Setter[ aliases.length ];
		for ( int i = 0; i < aliases.length; i++ ) {
			String alias = aliases[ i ];
			if ( alias != null ) {
				this.aliases[ i ] = alias;
				String nameProperty = getNameProperty(alias);
				setters[ i ] = propertyAccessor.getSetter( resultClass, nameProperty );
			}
		}
		isInitialized = true;
	}

	private String getNameProperty(String alias) {

		for(Field f:resultClass.getDeclaredFields()){
			if(f.getName().equalsIgnoreCase(alias)){
				return f.getName();
			}
			String aliasField = ""; // nombreFactura => NOMBRE_FACTURA
			for(int i=0; i<f.getName().length();i++){
				char c = f.getName().charAt(i);
				if(Character.isUpperCase(c)){
					aliasField += "_"+c;
				} else {
					aliasField += Character.toUpperCase(c);
				}
				
			}
			if(alias.equals(aliasField)){
				return f.getName();
			}
		}
		return alias;
	}

	private void check(String[] aliases) {
		if ( ! Arrays.equals( aliases, this.aliases ) ) {
			throw new IllegalStateException(
					"aliases are different from what is cached; aliases=" + Arrays.asList( aliases ) +
							" cached=" + Arrays.asList( this.aliases ) );
		}
	}

	public List transformList(List collection) {
		return collection;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AliasToBeanIgnoreCaseResultTransformer that = ( AliasToBeanIgnoreCaseResultTransformer ) o;

		if ( ! resultClass.equals( that.resultClass ) ) {
			return false;
		}
		if ( ! Arrays.equals( aliases, that.aliases ) ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = resultClass.hashCode();
		result = 31 * result + ( aliases != null ? Arrays.hashCode( aliases ) : 0 );
		return result;
	}
}
