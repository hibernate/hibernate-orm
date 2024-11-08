package org.hibernate.orm.test.annotations.join;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
@Table(name = "PAGES")
public class Page {
	private long pageId;

	@Id
	@GeneratedValue
	@Column(name = "PAGE_ID")
	public long getPageId() {
		return pageId;
	}

	public void setPageId(long groupId) {
		this.pageId = groupId;
	}
}

