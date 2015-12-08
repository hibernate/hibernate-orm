@Entity
public class Step {
	...
	@Lob
	@Basic
	public byte[] instructions;
	...
}