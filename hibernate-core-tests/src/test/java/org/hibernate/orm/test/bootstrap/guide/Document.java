package org.hibernate.orm.test.bootstrap.guide;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/// The mapped model shared by the User Guide bootstrap examples.
///
/// @author Steve Ebersole
//tag::bootstrap-guide-model[]
@Entity
@Table(name = "bootstrap_document")
public class Document {
	@Id
	private Long id;
	private String title;

	protected Document() {
	}

	public Document(Long id, String title) {
		this.id = id;
		this.title = title;
	}

	public String getTitle() {
		return title;
	}
}
//end::bootstrap-guide-model[]
