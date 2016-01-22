import java.util.List;


public class ServerProcess implements Comparable<ServerProcess> {

	protected int id;
	protected int priority;
	

	public ServerProcess(int id, int priority) {
		this.id = id;
		this.priority = priority;
	}
	
	public int getId(){
		return this.id;
	}
	
	public int getPriority(){
		return this.priority;
	}
	
	@Override
	public int compareTo(ServerProcess s) {
		if (this.priority < s.priority)
			return -1;
		if (this.priority > s.priority)
			return 1;
		
		return 0;
	}

	@Override
	public String toString(){
		return "(SERVER_" + id + "; PRIORITY: " + priority + ")";
	}
	
	@Override
	public boolean equals(Object obj) {	
		ServerProcess outro = (ServerProcess) obj;
		
		return (this.getId() == outro.getId());
	}
	
}
