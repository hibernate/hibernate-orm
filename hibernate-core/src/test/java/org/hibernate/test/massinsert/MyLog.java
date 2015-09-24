package org.hibernate.test.massinsert;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author chammer
 *
 */
@NamedQueries({
	@NamedQuery(name = "myloginsert",query = "insert into MyLog (text) select :text from MyLog")
})
@Entity
@Table(name="MyLog")
public class MyLog implements IContainer,Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue
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
