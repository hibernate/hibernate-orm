@Entity
public class Thing2 {
	@Id
	private Integer id;
	@Version
	private Instant ts;
	...
}