//$Id$
package org.hibernate.test.annotations.cid;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class TvMagazinPk implements Serializable {
	@ManyToOne
	public Channel channel;
	
	@ManyToOne
	public Presenter presenter;
}
