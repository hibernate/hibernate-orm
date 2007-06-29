//$Id: DependantValue.java 5685 2005-02-12 07:19:50Z steveebersole $
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
