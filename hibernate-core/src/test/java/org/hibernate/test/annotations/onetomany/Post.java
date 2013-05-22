package org.hibernate.test.annotations.onetomany;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
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