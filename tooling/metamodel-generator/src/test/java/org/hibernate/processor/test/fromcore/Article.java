/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.Locale;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Article")
public class Article {
	@Id
	Integer id;
	@OneToMany
	Map<Locale, Translation> translations;
}
