/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * @author Steve Ebersole
 */

@NamedEntityGraph( name = "book-title-isbn", graph = "Book: title, isbn")
@NamedEntityGraph( name = "book-title-isbn-author", graph = "Book: title, isbn, author")
@NamedEntityGraph( name = "book-title-isbn-editor", graph = "Book: title, isbn, editor")
@NamedEntityGraph( name = "duplicated-name", graph = "Book: title")
package org.hibernate.orm.test.entitygraph.named.parsed.pkg;

import org.hibernate.annotations.NamedEntityGraph;
