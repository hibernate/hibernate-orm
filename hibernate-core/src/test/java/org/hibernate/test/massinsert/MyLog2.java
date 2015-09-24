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
	@NamedQuery(name = "mylog2insert",query = "insert into MyLog2 (text) select :text from MyLog2")
})
@Entity
@Table(name="MyLog2")
public class MyLog2  implements IContainer,Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_GEN2")
	@SequenceGenerator(name="SEQ_GEN2",sequenceName="SEQ_GEN2",allocationSize=1)
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
