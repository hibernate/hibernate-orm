//$Id: Selectable.java 9908 2006-05-08 20:59:20Z max.andersen@jboss.com $
package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;

public interface Selectable {
	public String getAlias(Dialect dialect);
	public String getAlias(Dialect dialect, Table table);
	public boolean isFormula();
	public String getTemplate(Dialect dialect, SQLFunctionRegistry functionRegistry);
	public String getText(Dialect dialect);
	public String getText();
}
