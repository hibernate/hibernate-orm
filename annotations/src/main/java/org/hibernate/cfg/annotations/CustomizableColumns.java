package org.hibernate.cfg.annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import javax.persistence.Column;

import org.hibernate.annotations.Columns;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation" })
public class CustomizableColumns implements Columns {
	private final Column[] columns;

	public CustomizableColumns(Column[] columns) {
		this.columns = columns;
	}

	public Column[] columns() {
		return columns;
	}

	public Class<? extends Annotation> annotationType() {
		return Columns.class;
	}
}
