/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity(name = "Post")
@DiscriminatorValue(value = "WCT")
public class Post extends Comment{

	protected List<Comment> comments = new ArrayList<Comment>();

	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL , orphanRemoval = false, fetch = FetchType.LAZY)
	@LazyCollection(LazyCollectionOption.EXTRA)
	@OrderColumn(name = "idx")
	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
}
