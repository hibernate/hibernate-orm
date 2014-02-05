package org.hibernate.envers.test.integration.onetomany.bidirectional;

import javax.persistence.Entity;

import org.hibernate.envers.Audited;

@Audited
@Entity
public class Faq extends DomainObject {

	private String faqText;

	public String getFaqText() {
		return faqText;
	}

	public void setFaqText(String faqText) {
		this.faqText = faqText;
	}
}
