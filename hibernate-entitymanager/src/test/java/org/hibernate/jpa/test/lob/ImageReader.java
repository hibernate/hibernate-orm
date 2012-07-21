//$Id$
package org.hibernate.jpa.test.lob;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ImageReader implements Serializable {

	private long id;
	private Blob image;
	private Clob text;

	@Id
	@GeneratedValue
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Lob
	@Column(name = "bin_img")
	public Blob getImage() {
		return image;
	}

	public void setImage(Blob image) {
		this.image = image;
	}

	@Lob
	@Column(name = "img_text")
	public Clob getText() {
		return text;
	}

	public void setText(Clob text) {
		this.text = text;
	}
}
