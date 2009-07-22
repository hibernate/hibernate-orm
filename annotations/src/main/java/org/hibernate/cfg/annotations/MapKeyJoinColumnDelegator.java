package org.hibernate.cfg.annotations;

import java.lang.annotation.Annotation;
import javax.persistence.Column;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.JoinColumn;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation" })
public class MapKeyJoinColumnDelegator implements JoinColumn {
	private final MapKeyJoinColumn column;

	public MapKeyJoinColumnDelegator(MapKeyJoinColumn column) {
		this.column = column;
	}

	public String name() {
		return column.name();
	}

	public String referencedColumnName() {
		return column.referencedColumnName();
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

	public Class<? extends Annotation> annotationType() {
		return Column.class;
	}
}
