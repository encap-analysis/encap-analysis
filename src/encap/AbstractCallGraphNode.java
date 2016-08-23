package encap;

import java.util.Set;

public abstract class AbstractCallGraphNode {

	protected Set<AbstractCallGraphNode> callers = CommonUtils.newHashSet();
	protected Set<AbstractCallGraphNode> callees = CommonUtils.newHashSet();
	
	abstract void analyze();
	
	public abstract String toSimpleStr();

	public Set<AbstractCallGraphNode> getCallers() {
		return callers;
	}
	
	void setCallers(Set<AbstractCallGraphNode> callers) {
		this.callers = callers;
	}

	public Set<AbstractCallGraphNode> getCallees() {
		return callees;
	}
	
	void setCallees(Set<AbstractCallGraphNode> callees) {
		this.callees = callees;
	}
}
