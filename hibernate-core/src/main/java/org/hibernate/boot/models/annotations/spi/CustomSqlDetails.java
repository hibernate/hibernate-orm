/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.ResultCheckStyle;

/**
 * Commonality for annotations which define custom insert, update and delete SQL
 *
 * @author Steve Ebersole
 */
public interface CustomSqlDetails extends Annotation {
	String sql();

	void sql(String value);

	boolean callable();

	void callable(boolean value);

	String table();

	void table(String value);

	ResultCheckStyle check();

	void check(ResultCheckStyle resultCheckStyle);
}
