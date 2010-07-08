package org.hibernate.cfg.annotations;

import java.lang.annotation.Annotation;
import javax.persistence.Column;
import javax.persistence.MapKeyColumn;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation" })
public class MapKeyColumnDelegator implements Column {
	private final MapKeyColumn column;

	public MapKeyColumnDelegator(MapKeyColumn column) {
		this.column = column;
	}

	public String name() {
		return column.name();
	}

	public boolean unique() {
		return column.unique();
	}

	public boolean nullable() {
		return column.nullable();
	}

	public boolean insertable() {
		return column.insertable();
	}

	public boolean updatable() {
		return column.updatable();
	}

	public String columnDefinition() {
		return column.columnDefinition();
	}

	public String table() {
		return column.table();
	}

	public int length() {
		return column.length();
	}

	public int precision() {
		return column.precision();
	}

	public int scale() {
		return column.scale();
	}

	public Class<? extends Annotation> annotationType() {
		return Column.class;
	}
}
