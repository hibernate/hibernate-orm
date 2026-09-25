/// Named queries and statements registered through a package descriptor.
///
/// @author Steve Ebersole
@NamedQuery(
		name = "DescriptorBook.byTitle",
		query = "select b from DescriptorBook b where b.title = :title",
		resultClass = PackageDescriptorNamedQueriesTest.Book.class
)
@NamedNativeQuery(
		name = "DescriptorBook.byTitleNative",
		query = "select id, title from descriptor_book where title = ?",
		resultClass = PackageDescriptorNamedQueriesTest.Book.class
)
@NamedStatement(
		name = "DescriptorBook.rename",
		statement = "update DescriptorBook set title = :title where id = :id"
)
package org.hibernate.orm.test.jpa.nameddescriptor;

import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
