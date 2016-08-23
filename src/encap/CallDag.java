package encap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.MethodDeclaration;

public class CallDag {

	private CallGraph callgraph;
	
	private List<AbstractCallGraphNode> dagNodes = CommonUtils.newArrayList();
	
	public CallDag(CallGraph cg) {
		callgraph = cg;
		collapseCycles();
		
		int simple = 0, scc = 0;
		for (AbstractCallGraphNode acgn : dagNodes) {
			if (acgn instanceof SimpleCallGraphNode)
				++simple;
			else
				++scc;
		}
		CommonUtils.debug(simple + " simple nodes and " + scc + " sccs nodes are in the call DAG.");
	}
	
	// collapse SCCs into a single node
	// deap copy
	public void collapseCycles() {
		// copy verteres
		for (SimpleCallGraphNode cgn : callgraph.getNodes()) {
			this.dagNodes.add(new SimpleCallGraphNode(cgn));
		}
		
		// copy edges
		for (SimpleCallGraphNode cgn : callgraph.getNodes()) {
			SimpleCallGraphNode newCgn = findNodeByMtd(cgn.getMtd());
			
			Set<AbstractCallGraphNode> newCallers = CommonUtils.newHashSet();
			for (AbstractCallGraphNode caller : cgn.getCallers()) {
				AbstractCallGraphNode newCaller = findNodeByMtd(((SimpleCallGraphNode)caller).getMtd());
				newCallers.add(newCaller);
			}
			newCgn.setCallers(newCallers);
			
			Set<AbstractCallGraphNode> newCallees = CommonUtils.newHashSet();
			for (AbstractCallGraphNode callee : cgn.getCallees()) {
				AbstractCallGraphNode newCallee = findNodeByMtd(((SimpleCallGraphNode)callee).getMtd());
				newCallees.add(newCallee);
			}
			newCgn.setCallees(newCallees);
		}
		
		// collapse
		
		Set<AbstractCallGraphNode> toRemove = CommonUtils.newHashSet();
		
		for (List<AbstractCallGraphNode> scc : callgraph.getSccs()) {
			if (scc.size() == 1) {
				AbstractCallGraphNode acg = scc.get(0);
				if (!acg.callees.contains(acg) || !acg.callers.contains(acg))
					continue;
			}
			List<AbstractCallGraphNode> newScc = CommonUtils.newArrayList();
			for (AbstractCallGraphNode cgn : scc) {
				newScc.add(findNodeByMtd(((SimpleCallGraphNode)cgn).getMtd()));
			}
			
			SccCallGraphNode ccgn = new SccCallGraphNode(newScc);
			List<AbstractCallGraphNode> uniqueCallers = GraphUtils.getUniqueCallersOfScc(newScc);
			List<AbstractCallGraphNode> uniqueCallees = GraphUtils.getUniqueCalleesOfScc(newScc);
			ccgn.setCallers(new HashSet<>(uniqueCallers));
			ccgn.setCallees(new HashSet<>(uniqueCallees));
			
			for (AbstractCallGraphNode caller : uniqueCallees) {
				Set<AbstractCallGraphNode> callersOfCallee = caller.getCallers();
				callersOfCallee.add(ccgn);
			}
			
			for (AbstractCallGraphNode callee : uniqueCallers) {
				Set<AbstractCallGraphNode> calleesOfCaller = callee.getCallees();
				calleesOfCaller.add(ccgn);
			}
			
			dagNodes.add(ccgn);
			toRemove.addAll(newScc);
		}
		remove(toRemove);
	}
	
	List<AbstractCallGraphNode> getDagNodes() {
		return dagNodes;
	}
	
	SimpleCallGraphNode findNodeByMtd(MethodDeclaration mtd) {
		for (AbstractCallGraphNode cgn : dagNodes) {
			if (cgn instanceof SimpleCallGraphNode) {
				SimpleCallGraphNode scgn = (SimpleCallGraphNode)cgn;
				if (scgn.correspondTo(mtd)) {
					return scgn;
				}
			}
		}
		return null;
	}
	
	void remove(Collection<AbstractCallGraphNode> toRemove) {
		// remove
		for (AbstractCallGraphNode acgn : toRemove) {
			if (acgn instanceof SimpleCallGraphNode)
				dagNodes.remove(acgn);
		}
		
		// remove from caller and callees
		for (AbstractCallGraphNode cgn : dagNodes) {
			if (cgn instanceof SimpleCallGraphNode) {
				SimpleCallGraphNode scgn = (SimpleCallGraphNode) cgn;
				
				Iterator<AbstractCallGraphNode> callerIter = cgn.getCallers().iterator();
				while (callerIter.hasNext())
					if (toRemove.contains(callerIter.next()))
						callerIter.remove();
				
				Iterator<AbstractCallGraphNode> calleeIter = cgn.getCallees().iterator();
				while (calleeIter.hasNext())
					if (toRemove.contains(calleeIter.next()))
						calleeIter.remove();
			}
		}
	}
	
	void analyze() {
//		CommonUtils.println("^^^ Call graph before topsort ^^^");
//		CommonUtils.println(toString());
		
		detectCycles();
		
		if (hasCycle) {
			
		}
		
		List<AbstractCallGraphNode> l = GraphUtils.topsort(this);
		if (l == null) {
//			CommonUtils.debug("topsort error!");
//			CommonUtils.println("^^^ Call graph after topsort ^^^");
//			CommonUtils.println(toString());
			return;
		}

		// analyze according to the reverse order of topological sorting
		Collections.reverse(l);
		for (AbstractCallGraphNode cgn : l)
			cgn.analyze();
	}
	
	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CallDag: \n\n");
		for (AbstractCallGraphNode scgn : dagNodes) {
			sb.append(scgn.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	private boolean hasCycle = false;
	private List<List<AbstractCallGraphNode>> cycles = new ArrayList<>();
	
	public void detectCycles() {
		CommonUtils.println("====== Detecting cycles... ======");
		for (AbstractCallGraphNode node : dagNodes) {
			Set<AbstractCallGraphNode> visited = new HashSet<>();
			List<AbstractCallGraphNode> trace = new ArrayList<>();
			findCycles(node, visited, trace);
		}
		
		for (List<AbstractCallGraphNode> cycle : cycles) {
			CommonUtils.println("Cycle:");
			for (AbstractCallGraphNode acgn: cycle) {
				CommonUtils.print(acgn.toSimpleStr() +  " --> ");
			}
			CommonUtils.println("");
		}
		
		removeCycles();
	}
	
	public void removeCycles() {
		for (List<AbstractCallGraphNode> cycle : cycles) {
			for (AbstractCallGraphNode acgn: cycle) {
				acgn.getCallers().remove(acgn);
				acgn.getCallees().remove(acgn);
			}
		}
	}

	public void findCycles(AbstractCallGraphNode node, Set<AbstractCallGraphNode> visited, List<AbstractCallGraphNode> trace) {
		if (visited.contains(node)) {
			int index = trace.indexOf(node);
			if (index != -1) {
				hasCycle = true;
				List<AbstractCallGraphNode> cycle = new ArrayList<>();
				while (index < trace.size()) {
					cycle.add(trace.get(index));
					++index;
				}
				addAfterRotationTest(cycle);
				return;
			}
			return;
		}
		visited.add(node);
		trace.add(node);
		
		for (AbstractCallGraphNode callee : node.callees) {
			findCycles(callee, visited, trace);
		}
		
		trace.remove(trace.size()-1);
	}
	
	void addAfterRotationTest(List<AbstractCallGraphNode> cycle) {
		if (!cycles.isEmpty()) {
			for (int i = 1; i <= cycle.size(); ++i) {
				Collections.rotate(cycle, 1);
				if (cycles.contains(cycle)) {
					return;
				}
			}
		}
		cycles.add(cycle);
	}
}
