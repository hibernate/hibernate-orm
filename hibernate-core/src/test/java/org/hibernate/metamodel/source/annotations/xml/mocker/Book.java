package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

/**
 * @author Strong Liu
 */
@Entity
@TableGenerator(name = "TABLE_GEN", catalog = "ANNOTATION_CATALOG", schema = "ANNOTATION_SCHEMA")
public class Book {
	@Id
	@GeneratedValue(generator = "TABLE_GEN")
	private Long id;
	@Temporal(TemporalType.TIMESTAMP)
	private Date publishDate;
	@ManyToOne(cascade = CascadeType.DETACH)
	private Author author;
	@ElementCollection
	@AttributeOverride(name = "title", column = @Column(name = "TOC_TITLE"))
	private List<Topic> topics = new ArrayList<Topic>();

	public List<Topic> getTopics() {
		return topics;
	}

	public void setTopics(List<Topic> topics) {
		this.topics = topics;
	}

	@Version
	private Long version;

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
