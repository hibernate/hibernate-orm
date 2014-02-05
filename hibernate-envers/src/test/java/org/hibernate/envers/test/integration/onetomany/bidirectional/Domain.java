/**
 * 
 */
package org.hibernate.envers.test.integration.onetomany.bidirectional;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.AccessType;
import org.hibernate.envers.Audited;

@Audited
@Entity
public class Domain {
	
	/**
	 * Id
	 */
	@Id
	@AccessType("property")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "domain")
	private List<Faq> faqs = new ArrayList<Faq>();
	
	public List<Faq> getFaqs() {
		return faqs;
	}

	public void setFaqs(List<Faq> faqs) {
		this.faqs = faqs;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
