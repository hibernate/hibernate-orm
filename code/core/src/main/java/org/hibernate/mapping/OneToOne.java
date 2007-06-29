//$Id: OneToOne.java 7246 2005-06-20 20:32:36Z oneovthafew $
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.SpecialOneToOneType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * A one-to-one association mapping
 * @author Gavin King
 */
public class OneToOne extends ToOne {

	private boolean constrained;
	private ForeignKeyDirection foreignKeyType;
	private KeyValue identifier;
	private String propertyName;
	private String entityName;

	public OneToOne(Table table, PersistentClass owner) throws MappingException {
		super(table);
		this.identifier = owner.getKey();
		this.entityName = owner.getEntityName();
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ? null : propertyName.intern();
	}
	
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String propertyName) {
		this.entityName = entityName==null ? null : entityName.intern();
	}
	
	public Type getType() throws MappingException {
		if ( getColumnIterator().hasNext() ) {
			return new SpecialOneToOneType(
					getReferencedEntityName(), 
					foreignKeyType, 
					referencedPropertyName,
					isLazy(),
					isUnwrapProxy(),
					entityName,
					propertyName
				);
		}
		else {
			return TypeFactory.oneToOne( 
					getReferencedEntityName(), 
					foreignKeyType, 
					referencedPropertyName,
					isLazy(),
					isUnwrapProxy(),
					isEmbedded(),
					entityName,
					propertyName
				);
		}
	}

	public void createForeignKey() throws MappingException {
		if ( constrained && referencedPropertyName==null) {
			//TODO: handle the case of a foreign key to something other than the pk
			createForeignKeyOfEntity( ( (EntityType) getType() ).getAssociatedEntityName() );
		}
	}

	public java.util.List getConstraintColumns() {
		ArrayList list = new ArrayList();
		Iterator iter = identifier.getColumnIterator();
		while ( iter.hasNext() ) list.add( iter.next() );
		return list;
	}
	/**
	 * Returns the constrained.
	 * @return boolean
	 */
	public boolean isConstrained() {
		return constrained;
	}

	/**
	 * Returns the foreignKeyType.
	 * @return AssociationType.ForeignKeyType
	 */
	public ForeignKeyDirection getForeignKeyType() {
		return foreignKeyType;
	}

	/**
	 * Returns the identifier.
	 * @return Value
	 */
	public KeyValue getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the constrained.
	 * @param constrained The constrained to set
	 */
	public void setConstrained(boolean constrained) {
		this.constrained = constrained;
	}

	/**
	 * Sets the foreignKeyType.
	 * @param foreignKeyType The foreignKeyType to set
	 */
	public void setForeignKeyType(ForeignKeyDirection foreignKeyType) {
		this.foreignKeyType = foreignKeyType;
	}

	/**
	 * Sets the identifier.
	 * @param identifier The identifier to set
	 */
	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public boolean isNullable() {
		return !constrained;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
	
}
