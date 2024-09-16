/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import java.util.Locale;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

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
