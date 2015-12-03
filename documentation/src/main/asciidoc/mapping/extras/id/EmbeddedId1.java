@Entity
public class Login {
	@Embeddable
	public static class PK implements Serializable  {
		private String system;
		private String username;
		...
	}

	@EmbeddedId
	private PK pk;
	...
}