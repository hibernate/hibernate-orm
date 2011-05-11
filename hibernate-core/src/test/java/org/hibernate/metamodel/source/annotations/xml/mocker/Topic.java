package org.hibernate.metamodel.source.annotations.xml.mocker;

import javax.persistence.Embeddable;

/**
 * @author Strong Liu
 */
@Embeddable
public class Topic {
	private String title;
	private String summary;
	private int position;

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
