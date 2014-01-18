package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

/**
 * @author Oleksander Dukhno
 */
@Entity
public class Student {

	@Id
	private long id;
	@OneToMany(
			mappedBy = "student"
	)
	private List<StudentAnswer> answers;

	public Student() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<StudentAnswer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<StudentAnswer> answers) {
		this.answers = answers;
	}
}
