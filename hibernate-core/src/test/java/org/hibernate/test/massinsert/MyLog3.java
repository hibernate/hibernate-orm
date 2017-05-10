package org.hibernate.test.massinsert;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author chammer
 *
 */
@NamedQueries({
	@NamedQuery(name = "mylog3insert",query = "insert into MyLog3 (text) select :text from MyLog3")
})
@Entity
@Table(name="MyLog3")
public class MyLog3  implements IContainer,Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SEQ_GEN3")
	/**
	 * allocationSize other than 1 make the complete test to defunct
	 */
	@SequenceGenerator(name="SEQ_GEN3",sequenceName="SEQ_GEN3",allocationSize=50)
	protected Long id;
	@Column
	protected String text;

	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
