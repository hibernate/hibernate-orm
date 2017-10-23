/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name="CommentTable") // "Comment" reserved in Oracle
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE", discriminatorType= DiscriminatorType.STRING, length = 3)
@DiscriminatorValue(value = "WPT")
public class Comment {
	
	private Long id;
	private Post post;
	private String name;
	private Forum forum;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, insertable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@ManyToOne(optional=true,fetch=FetchType.LAZY)
	@JoinColumn(name="FK_PostId", nullable=true, insertable=true,updatable=false)
	public Post getPost() {
		return post;
	}

	public void setPost(Post family) {
		this.post = family;
	}

	@ManyToOne(optional=true,fetch=FetchType.LAZY)
	@JoinColumn(name="FK_ForumId", nullable=true, insertable=true,updatable=false)
	public Forum getForum() {
		return forum;
	}

	public void setForum(Forum forum) {
		this.forum = forum;
	}

	@Column
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
