/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.mapping;
import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.Mappings;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A mapping for a one-to-many association
 * @author Gavin King
 */
public class OneToMany implements Value {

	private final Mappings mappings;
	private final Table referencingTable;

	private String referencedEntityName;
	private PersistentClass associatedClass;
	private boolean embedded;
	private boolean ignoreNotFound;

	private EntityType getEntityType() {
		return mappings.getTypeResolver().getTypeFactory().manyToOne(
				getReferencedEntityName(), 
				null, 
				false,
				false,
				isEmbedded(),
				isIgnoreNotFound(),
				false
			);
	}

	public OneToMany(Mappings mappings, PersistentClass owner) throws MappingException {
		this.mappings = mappings;
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
