@Entity
public class Step {
	...
	@Lob
	@Basic
	public Blob instructions;
	...
}