//$Id: ManyToOne.java 7586 2005-07-21 01:11:52Z oneovthafew $
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * A many-to-one association mapping
 * @author Gavin King
 */
public class ManyToOne extends ToOne {
	
	private boolean ignoreNotFound;
	
	public ManyToOne(Table table) {
		super(table);
	}

	public Type getType() throws MappingException {
		return TypeFactory.manyToOne( 
				getReferencedEntityName(), 
				getReferencedPropertyName(),
				isLazy(),
				isUnwrapProxy(),
				isEmbedded(),
				isIgnoreNotFound()
			);
	}

	public void createForeignKey() throws MappingException {
		// the case of a foreign key to something other than the pk is handled in createPropertyRefConstraints
		if (referencedPropertyName==null && !hasFormula() ) {
			createForeignKeyOfEntity( ( (EntityType) getType() ).getAssociatedEntityName() );
		} 
	}

	public void createPropertyRefConstraints(Map persistentClasses) {
		if (referencedPropertyName!=null) {
			PersistentClass pc = (PersistentClass) persistentClasses.get(getReferencedEntityName() );
			
			Property property = pc.getReferencedProperty( getReferencedPropertyName() );
			
			if (property==null) {
				throw new MappingException(
						"Could not find property " + 
						getReferencedPropertyName() + 
						" on " + 
						getReferencedEntityName() 
					);
			} 
			else {
				if ( !hasFormula() && !"none".equals( getForeignKeyName() ) ) {
					java.util.List refColumns = new ArrayList();
					Iterator iter = property.getColumnIterator();
					while ( iter.hasNext() ) {
						Column col = (Column) iter.next();
						refColumns.add( col );							
					}
					
					ForeignKey fk = getTable().createForeignKey( 
							getForeignKeyName(), 
							getConstraintColumns(), 
							( (EntityType) getType() ).getAssociatedEntityName(), 
							refColumns 
						);
					fk.setCascadeDeleteEnabled(isCascadeDeleteEnabled() );
				}
			}
		}
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

	
}
