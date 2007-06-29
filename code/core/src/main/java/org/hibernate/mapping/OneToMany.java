//$Id: OneToMany.java 7246 2005-06-20 20:32:36Z oneovthafew $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * A mapping for a one-to-many association
 * @author Gavin King
 */
public class OneToMany implements Value {

	private String referencedEntityName;
	private Table referencingTable;
	private PersistentClass associatedClass;
	private boolean embedded;
	private boolean ignoreNotFound;

	private EntityType getEntityType() {
		return TypeFactory.manyToOne(
				getReferencedEntityName(), 
				null, 
				false,
				false,
				isEmbedded(),
				isIgnoreNotFound()
			);
	}

	public OneToMany(PersistentClass owner) throws MappingException {
		this.referencingTable = (owner==null) ? null : owner.getTable();
	}

	public PersistentClass getAssociatedClass() {
		return associatedClass;
	}

    /**
     * Associated entity on the many side
     */
	public void setAssociatedClass(PersistentClass associatedClass) {
		this.associatedClass = associatedClass;
	}

	public void createForeignKey() {
		// no foreign key element of for a one-to-many
	}

	public Iterator getColumnIterator() {
		return associatedClass.getKey().getColumnIterator();
	}

	public int getColumnSpan() {
		return associatedClass.getKey().getColumnSpan();
	}

	public FetchMode getFetchMode() {
		return FetchMode.JOIN;
	}

    /** 
     * Table of the owner entity (the "one" side)
     */
	public Table getTable() {
		return referencingTable;
	}

	public Type getType() {
		return getEntityType();
	}

	public boolean isNullable() {
		return false;
	}

	public boolean isSimpleValue() {
		return false;
	}

	public boolean isAlternateUniqueKey() {
		return false;
	}

	public boolean hasFormula() {
		return false;
	}
	
	public boolean isValid(Mapping mapping) throws MappingException {
		if (referencedEntityName==null) {
			throw new MappingException("one to many association must specify the referenced entity");
		}
		return true;
	}

    public String getReferencedEntityName() {
		return referencedEntityName;
	}

    /** 
     * Associated entity on the "many" side
     */    
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ? null : referencedEntityName.intern();
	}

	public void setTypeUsingReflection(String className, String propertyName) {}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
	
	
	public boolean[] getColumnInsertability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}
	
	public boolean[] getColumnUpdateability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}
	
	public boolean isEmbedded() {
		return embedded;
	}
	
	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}
	
}
