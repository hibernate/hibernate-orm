/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */


@NamedEntityGraph(root = Book.class, name = "book-title-with-root-attribute", graph = "title")
@NamedEntityGraph(root = Book.class, name = "book-title-author-editor-with-root-attribute", graph = "title, author, editor")
package org.hibernate.orm.test.entitygraph.named.parsed.pkg3;

import org.hibernate.annotations.NamedEntityGraph;
