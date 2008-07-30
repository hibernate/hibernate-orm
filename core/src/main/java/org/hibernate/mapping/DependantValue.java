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
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.type.Type;

/**
 * A value which is "typed" by reference to some other
 * value (for example, a foreign key is typed by the
 * referenced primary key).
 *
 * @author Gavin King
 */
public class DependantValue extends SimpleValue {
	private KeyValue wrappedValue;
	private boolean nullable;
	private boolean updateable;

	public DependantValue(Table table, KeyValue prototype) {
		super(table);
		this.wrappedValue = prototype;
	}

	public Type getType() throws MappingException {
		return wrappedValue.getType();
	}

	public void setTypeUsingReflection(String className, String propertyName) {}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public boolean isNullable() {
		return nullable;
	
	}
	
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	
	public boolean isUpdateable() {
		return updateable;
	}
	
	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}
}
