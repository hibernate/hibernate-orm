/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.util;

import javax.swing.table.AbstractTableModel;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class BeanTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 
			ObjectStreamClass.lookup(BeanTableModel.class).getSerialVersionUID();
	

	protected List<PropertyDescriptor> list;

	private BeanInfo beanInfo = null;

	private PropertyDescriptor[] descriptors = null;
	
	public BeanTableModel(List<PropertyDescriptor> list, Class<?> beanClass) {
		this.list = list;
		introspect( beanClass );
	}

	private void introspect(Class<?> beanClass) {
		try {
			this.beanInfo = Introspector.getBeanInfo( beanClass,
					Introspector.USE_ALL_BEANINFO );
			//descriptor = beanInfo.getBeanDescriptor();
			descriptors = beanInfo.getPropertyDescriptors();
		}
		catch (IntrospectionException ie) {
			// ignore
		}
		
		List<PropertyDescriptor> v = new ArrayList<PropertyDescriptor>(descriptors.length);
		for (int i = 0; i < descriptors.length; i++) {
			if(!descriptors[i].getName().equals("class")) {
				v.add( descriptors[i] );
			}			
		}
		descriptors = (PropertyDescriptor[]) v.toArray( new PropertyDescriptor[v.size()] );

	}

	boolean isSingle() {
		return list.size()<=1;
	}
	
	public int getRowCount() {
		return isSingle() ? descriptors.length : list.size();
	}

	public int getColumnCount() {
		return isSingle() ? list.size() + 1 : (descriptors != null ? descriptors.length : 0);
	}

	public Object getValueAt(int row, int col) {
		if(isSingle()) {
			if(col==0) {
				return descriptors[row].getDisplayName();
			} else {
				return getValue(0, row);
			}
		} else {
			return getValue( row, col );
		}
	}

	private Object getValue(int row, int col) {
		Object bean = list.get( row );
		Object result = null;
		try {
			result = descriptors[col].getReadMethod().invoke( bean, (Object[])null );
		}
		catch (InvocationTargetException ite) {
			ite.printStackTrace();
		}
		catch (IllegalAccessException iae) {
			iae.printStackTrace();
		}
		return result;
	}

	public String getColumnName(int col) {
		if(isSingle()) {
			if(col==0) {
				return "Name";
			} else {
				return "Value";
			}
		} else {
			return descriptors[col].getDisplayName();
		}
	}

	public Class<?> getColumnClass(int c) {
		if(isSingle()) {
			return String.class;
		} else {
			Class<?> propertyType = descriptors[c].getPropertyType();

			if(propertyType.isPrimitive()) {
				return String.class; // to avoid jtable complain about null table renderer.
			} else {
				return propertyType;
			}
		}
	}
}