package org.hibernate.tool.internal.reveng.binder;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;

public class VersionPropertyBinder extends AbstractBinder {
	
	private final static Logger LOGGER = Logger.getLogger(VersionPropertyBinder.class.getName());
	
	public static VersionPropertyBinder create(BinderContext binderContext) {
		return new VersionPropertyBinder(binderContext);
	}
	
	private final BasicPropertyBinder basicPropertyBinder;
	
	private VersionPropertyBinder(BinderContext binderContext) {
		super(binderContext);
		this.basicPropertyBinder = BasicPropertyBinder.create(binderContext);
	}
	
	public void bind(
			Table table, 
			RootClass rc, 
			Set<Column> processed, 
			Mapping mapping) {
		TableIdentifier identifier = TableIdentifier.create(table);
		String optimisticLockColumnName = getRevengStrategy().getOptimisticLockColumnName(identifier);
		if(optimisticLockColumnName!=null) {
			Column column = table.getColumn(new Column(optimisticLockColumnName));
			if(column==null) {
				LOGGER.log(Level.WARNING, "Column " + column + " wanted for <version>/<timestamp> not found in " + identifier);
			} else {
				bindVersionProperty(table, identifier, column, rc, processed, mapping);
			}
		} else {
			LOGGER.log(Level.INFO, "Scanning " + identifier + " for <version>/<timestamp> columns.");
			Iterator<?> columnIterator = table.getColumnIterator();
			while(columnIterator.hasNext()) {
				Column column = (Column) columnIterator.next();
				boolean useIt = getRevengStrategy().useColumnForOptimisticLock(identifier, column.getName());
				if(useIt && !processed.contains(column)) {
					bindVersionProperty( table, identifier, column, rc, processed, mapping );
					return;
				}
			}
			LOGGER.log(Level.INFO, "No columns reported while scanning for <version>/<timestamp> columns in " + identifier);
		}
	}

	private void bindVersionProperty(
			Table table, 
			TableIdentifier identifier, 
			Column column, 
			RootClass rc, 
			Set<Column> processed, 
			Mapping mapping) {
		processed.add(column);
		String propertyName = getRevengStrategy().columnToPropertyName( identifier, column.getName() );
		Property property = basicPropertyBinder.bind(
				BinderUtils.makeUnique(rc, propertyName), 
				table, 
				column);
		rc.addProperty(property);
		rc.setVersion(property);
		rc.setOptimisticLockStyle(OptimisticLockStyle.VERSION);
		LOGGER.log(Level.INFO, "Column " + column.getName() + " will be used for <version>/<timestamp> columns in " + identifier);

	}
}
