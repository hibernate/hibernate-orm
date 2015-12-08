@Entity
public class Login {
	@Embeddable
	public static class PK implements Serializable {
		@ManyToOne
		private System system;
		private String username;
		...
	}

	@EmbeddedId
	private PK pk;
	...
}