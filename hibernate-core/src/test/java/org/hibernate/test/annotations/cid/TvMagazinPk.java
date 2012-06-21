//$Id$
package org.hibernate.test.annotations.cid;
import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class TvMagazinPk implements Serializable {
	@ManyToOne
	@JoinColumn(nullable=false)
	public Channel channel;
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Presenter presenter;
}
