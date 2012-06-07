package org.hibernate.test.insertordering;
import java.util.Date;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Membership {
	private Long id;
	private User user;
	private Group group;
	private Date activationDate;

	/**
	 * For persistence
	 */
	Membership() {
	}

	public Membership(User user, Group group) {
		this( user, group, new Date() );
	}

	public Membership(User user, Group group, Date activationDate) {
		this.user = user;
		this.group = group;
		this.activationDate = activationDate;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Group getGroup() {
		return group;
	}

	public Date getActivationDate() {
		return activationDate;
	}
}
