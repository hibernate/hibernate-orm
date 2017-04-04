/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;
import java.lang.annotation.Annotation;
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
