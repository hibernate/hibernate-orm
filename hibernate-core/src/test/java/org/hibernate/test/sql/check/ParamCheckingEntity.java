package org.hibernate.test.sql.check;


/**
 * An entity which is expected to be mapped to each database using stored
 * procedures which return "affected row counts"; in other words, using
 * {@link org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle#PARAM}.
 *
 * @author Steve Ebersole
 */
public class ParamCheckingEntity {
	private Long id;
	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
