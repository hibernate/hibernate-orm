package org.hibernate.tool.internal.reveng.binder;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;

class VersionPropertyBinder extends AbstractBinder {
	
	private final static Logger LOGGER = Logger.getLogger(VersionPropertyBinder.class.getName());
	
	static VersionPropertyBinder create(BinderContext binderContext) {
		return new VersionPropertyBinder(binderContext);
	}
	
	private final BasicPropertyBinder basicPropertyBinder;
	
	private VersionPropertyBinder(BinderContext binderContext) {
		super(binderContext);
		this.basicPropertyBinder = BasicPropertyBinder.create(binderContext);
	}
	
	void bind(
			Table table, 
			RootClass rc, 
			Set<Column> processed) {
		String optimisticLockColumnName = getRevengStrategy()
				.getOptimisticLockColumnName(TableIdentifier.create(table));
		if(optimisticLockColumnName!=null) {
			handleSpecifiedVersionColumn(table, optimisticLockColumnName, rc, processed);
		} else {
			scanForAppropriateVersionColumn(table, rc, processed);
		}
	}
	
	private void scanForAppropriateVersionColumn(
			Table table,
			RootClass rc, 
			Set<Column> processed) {
		TableIdentifier identifier = TableIdentifier.create(table);
		LOGGER.log(Level.INFO, "Scanning " + identifier + " for <version>/<timestamp> columns.");
		for (Column column : table.getColumns()) {
			boolean useIt = getRevengStrategy().useColumnForOptimisticLock(identifier, column.getName());
			if(useIt && !processed.contains(column)) {
				bindVersionProperty(table, column, rc, processed);
				return;
			}
		}
		LOGGER.log(Level.INFO, "No columns reported while scanning for <version>/<timestamp> columns in " + identifier);
	}
	
	private void handleSpecifiedVersionColumn(
			Table table,
			String optimisticLockColumnName,
			RootClass rc, 
			Set<Column> processed) {
		TableIdentifier identifier = TableIdentifier.create(table);
		Column column = table.getColumn(new Column(optimisticLockColumnName));
		if(column==null) {
			LOGGER.log(Level.WARNING, "Column " + column + " wanted for <version>/<timestamp> not found in " + identifier);
		} else {
			bindVersionProperty(table, column, rc, processed);
		}	
	}

	private void bindVersionProperty(
			Table table, 
			Column column, 
			RootClass rc, 
			Set<Column> processed) {
		TableIdentifier identifier = TableIdentifier.create(table);
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
