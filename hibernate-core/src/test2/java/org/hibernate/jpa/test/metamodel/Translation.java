/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.metamodel;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class Translation {
	@Id
	Integer id;
	String title;
	String text;
}
